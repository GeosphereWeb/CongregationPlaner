// Prüft ../firestore.rules gegen den Firestore-Emulator mit echten Auth-Tokens (Auth-Emulator).
// Aufruf siehe README.md; das Skript braucht die laufenden Emulatoren aus firebase.json.

const PROJECT = 'demo-rules-test';
const FS = `http://127.0.0.1:18080/v1/projects/${PROJECT}/databases/(default)/documents`;
const AUTH = 'http://127.0.0.1:19099/identitytoolkit.googleapis.com/v1';
const CONG = 'region/R/subregion/S/branch/B/circuit/C/congregation/K';

const failures = [];
let count = 0;

function enc(value) {
  if (value === null) return { nullValue: null };
  if (typeof value === 'boolean') return { booleanValue: value };
  if (typeof value === 'number') return { integerValue: String(value) };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(enc) } };
  return { stringValue: value };
}

const fields = (data) => ({ fields: Object.fromEntries(Object.entries(data).map(([key, value]) => [key, enc(value)])) });

async function call(method, url, body, token) {
  const headers = { 'Content-Type': 'application/json' };
  if (token) headers.Authorization = `Bearer ${token}`;
  const response = await fetch(url, { method, headers, body: body === undefined ? undefined : JSON.stringify(body) });
  return { status: response.status, text: await response.text() };
}

const api = (method, path, body, token) => call(method, `${FS}/${path}`, body, token);

async function signUp(email) {
  const { status, text } = await call('POST', `${AUTH}/accounts:signUp?key=fake`, {
    email,
    password: 'secret12',
    returnSecureToken: true,
  });
  if (status !== 200) throw new Error(`Sign-up failed: ${text}`);
  const data = JSON.parse(text);
  return { uid: data.localId, token: data.idToken };
}

/** allowed = true erwartet 200, allowed = false erwartet 403 (die Rules lehnen ab); jede andere Antwort ist ein Fehler. */
async function expect(name, pending, allowed) {
  count += 1;
  const { status, text } = await pending;
  const ok = allowed ? status === 200 : status === 403;
  if (!ok) failures.push(`${name}: expected ${allowed ? 200 : 403}, got ${status} ${text.slice(0, 160)}`);
}

const memberDoc = (email, permissions) =>
  fields({ email, displayName: email, approved: true, role: 'MEMBER', permissions });

// ---------- Nutzer und Daten (Seeding mit dem Owner-Token umgeht die Rules) ----------
const admin = await signUp('admin@test.de');
const planner = await signUp('planner@test.de');
const viewer = await signUp('viewer@test.de');
const plain = await signUp('plain@test.de');

const owner = 'owner';
await api('PATCH', `admins/${admin.uid}`, { fields: {} }, owner);
await api('PATCH', `${CONG}/members/${planner.uid}`, memberDoc('planner@test.de', ['DUMMY', 'PLAN_TALKS']), owner);
await api('PATCH', `${CONG}/members/${viewer.uid}`, memberDoc('viewer@test.de', ['PLAN_TALKS', 'VIEW_MEMBERS']), owner);
await api('PATCH', `${CONG}/members/${plain.uid}`, memberDoc('plain@test.de', ['DUMMY']), owner);

const SLOT = `${CONG}/talkSchedule`;
const base = { time: '17:30', talkCancelled: false };
const speaker = { speakerMemberId: 'm1', speakerLastName: 'Muster', speakerFirstName: 'Max' };
const chairman = { chairmanMemberId: 'm2', chairmanLastName: 'Alt', chairmanFirstName: 'Anton' };
const external = {
  externalSpeakerId: 'e1',
  speakerLastName: 'Extern',
  speakerFirstName: 'Eva',
  speakerCongregation: 'Nord',
};
const externalWithoutCongregation = Object.fromEntries(
  Object.entries(external).filter(([key]) => key !== 'speakerCongregation'),
);

const putSlot = (day, data, token = planner.token) => api('PATCH', `${SLOT}/${day}`, fields(data), token);

// ---------- Termine ----------
await expect('Termin minimal', putSlot('2026-10-25', base), true);
await expect('Termin mit Mitglied als Redner und Vorsitzendem', putSlot('2026-10-25', { ...base, ...speaker, ...chairman }), true);
await expect(
  'Redner = Vorsitzender',
  putSlot('2026-10-25', { ...base, ...speaker, chairmanMemberId: 'm1', chairmanLastName: 'Muster', chairmanFirstName: 'Max' }),
  false,
);
await expect(
  'Mitglied und externer Redner zugleich',
  putSlot('2026-10-25', { ...base, ...speaker, externalSpeakerId: 'e1', speakerCongregation: 'Nord' }),
  false,
);
await expect('externer Redner', putSlot('2026-10-25', { ...base, ...external }), true);
await expect('externer Redner mit Vorsitzendem', putSlot('2026-10-25', { ...base, ...external, ...chairman }), true);
await expect('externer Redner ohne Versammlung', putSlot('2026-10-25', { ...base, ...externalWithoutCongregation }), false);
await expect('Versammlung ohne externen Redner', putSlot('2026-10-25', { ...base, ...speaker, speakerCongregation: 'Nord' }), false);
await expect('Redner-Namen ohne ID', putSlot('2026-10-25', { ...base, speakerLastName: 'Muster', speakerFirstName: 'Max' }), false);
await expect('Redner-ID ohne Namen', putSlot('2026-10-25', { ...base, speakerMemberId: 'm1' }), false);
await expect('Vorsitzender unvollständig', putSlot('2026-10-25', { ...base, chairmanMemberId: 'm2', chairmanLastName: 'Alt' }), false);
await expect('Vortrag mit Nummer und Titel', putSlot('2026-10-25', { ...base, talkNumber: 12, talkTitle: 'Der Weg zum Glück' }), true);
await expect('Vortrag nur mit Nummer', putSlot('2026-10-25', { ...base, talkNumber: 12 }), false);
await expect('Vortragsnummer 0', putSlot('2026-10-25', { ...base, talkNumber: 0, talkTitle: 'x' }), false);
await expect('Wochenereignis mit ID und Name', putSlot('2026-10-25', { ...base, weekEventId: 'k', weekEventName: 'Kongress' }), true);
await expect('Wochenereignis nur mit ID', putSlot('2026-10-25', { ...base, weekEventId: 'k' }), false);
await expect('ungültiges Datum 2026-13-01', putSlot('2026-13-01', base), false);
await expect('ungültiges Datum 2026-2-1', putSlot('2026-2-1', base), false);
await expect('unbekanntes Feld', putSlot('2026-10-25', { ...base, extra: 'x' }), false);
await expect('ungültige Uhrzeit', putSlot('2026-10-25', { time: '25:00', talkCancelled: false }), false);
await expect('Planer liest Termin', api('GET', `${SLOT}/2026-10-25`, undefined, planner.token), true);
await expect('Nutzer ohne PLAN_TALKS schreibt Termin', putSlot('2026-11-01', base, plain.token), false);
await expect('Nutzer ohne PLAN_TALKS liest Termin', api('GET', `${SLOT}/2026-10-25`, undefined, plain.token), false);
await expect('Planer löscht Termin', api('DELETE', `${SLOT}/2026-10-25`, undefined, planner.token), true);

// ---------- Externe Redner ----------
const EXT = `${CONG}/externalSpeakers`;
const validExternal = { lastName: 'Extern', firstName: 'Eva', congregationName: 'Nord' };
await expect('externen Redner anlegen', call('POST', `${FS}/${EXT}`, fields(validExternal), planner.token), true);
await expect('externer Redner ohne Versammlung', call('POST', `${FS}/${EXT}`, fields({ lastName: 'Extern', firstName: 'Eva' }), planner.token), false);
await expect('externer Redner mit leerem Familiennamen', call('POST', `${FS}/${EXT}`, fields({ ...validExternal, lastName: '' }), planner.token), false);
await expect('externer Redner mit Zusatzfeld', call('POST', `${FS}/${EXT}`, fields({ ...validExternal, x: 'y' }), planner.token), false);
await expect('externe Redner lesen als Planer', api('GET', EXT, undefined, planner.token), true);
await expect('externe Redner lesen ohne PLAN_TALKS', api('GET', EXT, undefined, plain.token), false);
await expect('externen Redner anlegen ohne PLAN_TALKS', call('POST', `${FS}/${EXT}`, fields(validExternal), plain.token), false);

// ---------- Vortragshistorie ----------
const HIST = `${CONG}/talkHistory`;
const putHistory = (number, data, token = planner.token) => api('PATCH', `${HIST}/${number}`, fields(data), token);
await expect('Vortragshistorie anlegen', putHistory('7', { heldOn: ['2026-03-01', '2026-10-25'] }), true);
await expect('Vortragshistorie mit geplanten Tagen', putHistory('6', { heldOn: ['2026-03-01'], plannedOn: ['2026-03-01'] }), true);
await expect('Vortragshistorie mit ungültigen geplanten Tagen', putHistory('6', { heldOn: ['2026-03-01'], plannedOn: 'x' }), false);
await expect('Vortragshistorie mit ungültiger Nummer', putHistory('abc', { heldOn: ['2026-03-01'] }), false);
await expect('Vortragshistorie ohne Tage', putHistory('8', { heldOn: [] }), false);
await expect('Vortragshistorie mit Zusatzfeld', putHistory('8', { heldOn: ['2026-03-01'], x: 'y' }), false);
await expect('Vortragshistorie lesen als Planer', api('GET', HIST, undefined, planner.token), true);
await expect('Vortragshistorie lesen ohne PLAN_TALKS', api('GET', HIST, undefined, plain.token), false);
await expect('Vortragshistorie anlegen ohne PLAN_TALKS', putHistory('9', { heldOn: ['2026-03-01'] }, plain.token), false);
await expect('Planer löscht Vortragshistorie', api('DELETE', `${HIST}/7`, undefined, planner.token), true);

// ---------- Mitglieder lesen (VIEW_MEMBERS) ----------
const MEMBERS = `${CONG}/members`;
await expect('Planer ohne VIEW_MEMBERS listet Mitglieder', api('GET', MEMBERS, undefined, planner.token), false);
await expect('Planer liest das eigene Mitglied', api('GET', `${MEMBERS}/${planner.uid}`, undefined, planner.token), true);
await expect('Planer ohne VIEW_MEMBERS liest ein fremdes Mitglied', api('GET', `${MEMBERS}/${plain.uid}`, undefined, planner.token), false);
await expect('VIEW_MEMBERS listet Mitglieder', api('GET', MEMBERS, undefined, viewer.token), true);
await expect('VIEW_MEMBERS liest ein fremdes Mitglied', api('GET', `${MEMBERS}/${plain.uid}`, undefined, viewer.token), true);
await expect(
  'VIEW_MEMBERS schreibt ein Mitglied nicht',
  api('PATCH', `${MEMBERS}/${plain.uid}?updateMask.fieldPaths=approved`, { fields: { approved: { booleanValue: false } } }, viewer.token),
  false,
);
await expect('Nutzer ohne Rechte listet Mitglieder', api('GET', MEMBERS, undefined, plain.token), false);
await expect('Global Admin listet Mitglieder', api('GET', MEMBERS, undefined, admin.token), true);

// ---------- Vortragsliste und Wochenereignisse ----------
const talk = (title = 'T', active = true) => fields({ title, active });
await expect('Vortragsliste lesen als angemeldeter Nutzer', api('GET', 'talkCatalog', undefined, plain.token), true);
await expect('Vortragsliste lesen ohne Anmeldung', api('GET', 'talkCatalog'), false);
await expect('Vortrag schreiben als normaler Nutzer', api('PATCH', 'talkCatalog/12', talk(), plain.token), false);
await expect('Vortrag schreiben als Planer', api('PATCH', 'talkCatalog/12', talk(), planner.token), false);
await expect('Vortrag schreiben als Global Admin', api('PATCH', 'talkCatalog/12', talk('Der Weg'), admin.token), true);
await expect('Vortrag Nummer 0', api('PATCH', 'talkCatalog/0', talk(), admin.token), false);
await expect('Vortrag Nummer 012', api('PATCH', 'talkCatalog/012', talk(), admin.token), false);
await expect('Vortrag Nummer 10000', api('PATCH', 'talkCatalog/10000', talk(), admin.token), false);
await expect('Vortrag ohne active', api('PATCH', 'talkCatalog/13', fields({ title: 'T' }), admin.token), false);
await expect('Vortrag mit leerem Titel', api('PATCH', 'talkCatalog/13', talk(''), admin.token), false);
await expect('Vortrag löschen als Global Admin', api('DELETE', 'talkCatalog/12', undefined, admin.token), true);
await expect('Vortrag löschen als normaler Nutzer', api('DELETE', 'talkCatalog/12', undefined, plain.token), false);
await expect('Wochenereignis schreiben als Global Admin', api('PATCH', 'weekEvents/k', fields({ name: 'Kongress', cancelsTalk: true }), admin.token), true);
await expect('Wochenereignis ohne cancelsTalk', api('PATCH', 'weekEvents/k2', fields({ name: 'Kongress' }), admin.token), false);
await expect('Wochenereignis mit leerem Namen', api('PATCH', 'weekEvents/k2', fields({ name: '', cancelsTalk: false }), admin.token), false);
await expect('Wochenereignis schreiben als normaler Nutzer', api('PATCH', 'weekEvents/k3', fields({ name: 'X', cancelsTalk: false }), plain.token), false);
await expect('Wochenereignisse lesen als angemeldeter Nutzer', api('GET', 'weekEvents', undefined, plain.token), true);
await expect('Wochenereignis löschen als Global Admin', api('DELETE', 'weekEvents/k', undefined, admin.token), true);

console.log(`${count} checks, ${failures.length} failures`);
failures.forEach((failure) => console.log(`FAILURE: ${failure}`));
process.exitCode = failures.length === 0 ? 0 : 1;
