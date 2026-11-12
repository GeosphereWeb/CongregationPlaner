// Prüft ../firestore.rules gegen den Firestore-Emulator mit echten Auth-Tokens (Auth-Emulator).
// Aufruf siehe README.md; das Skript braucht die laufenden Emulatoren aus firebase.json.

const PROJECT = 'demo-rules-test';
const FS = `http://127.0.0.1:18080/v1/projects/${PROJECT}/databases/(default)/documents`;
const AUTH = 'http://127.0.0.1:19099/identitytoolkit.googleapis.com/v1';
const CIRCUIT = 'region/R/subregion/S/branch/B/country/L/circuit/C';
const CONG = `${CIRCUIT}/congregation/K`;

const failures = [];
let count = 0;

function enc(value) {
  if (value === null) return { nullValue: null };
  if (typeof value === 'boolean') return { booleanValue: value };
  if (typeof value === 'number') return { integerValue: String(value) };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(enc) } };
  if (typeof value === 'object') return { mapValue: fields(value) };
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

// Mehrere Schreibzugriffe atomar, wie der Batch der App; die Rules sehen den Zustand danach (getAfter/existsAfter).
const DOCS = `projects/${PROJECT}/databases/(default)/documents`;
const setDoc = (path, data) => ({ update: { name: `${DOCS}/${path}`, ...fields(data) } });
const delDoc = (path) => ({ delete: `${DOCS}/${path}` });
const commit = (writes, token) => call('POST', `${FS}:commit`, { writes }, token);

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

/** Nutzer mit bestätigter E-Mail-Adresse: der Auth-Emulator setzt das Flag, der neue Token trägt `email_verified`. */
async function signUpVerified(email) {
  const user = await signUp(email);
  const marked = await call('POST', `${AUTH.replace('/v1', '/v1/projects/' + PROJECT)}/accounts:update`,
    { localId: user.uid, emailVerified: true }, 'owner');
  if (marked.status !== 200) throw new Error(`Verifying the e-mail failed: ${marked.text}`);
  const signedIn = await call('POST', `${AUTH}/accounts:signInWithPassword?key=fake`,
    { email, password: 'secret12', returnSecureToken: true });
  return { uid: user.uid, token: JSON.parse(signedIn.text).idToken };
}

/** allowed = true erwartet 200, allowed = false erwartet 403 (die Rules lehnen ab); jede andere Antwort ist ein Fehler. */
async function expect(name, pending, allowed) {
  count += 1;
  const { status, text } = await pending;
  const ok = allowed ? status === 200 : status === 403;
  if (!ok) failures.push(`${name}: expected ${allowed ? 200 : 403}, got ${status} ${text.slice(0, 160)}`);
}

// Vollständiges Mitgliedsdokument: die Rules verlangen alle Profilfelder, die App schreibt sie immer.
const fullProfile = {
  lastName: '', firstName: '', category: null, baptized: false, appointment: 'NONE', mobilePhone: '', landlinePhone: '',
  isSpeaker: false, emails: [], street: '', postalCode: '', city: '', emergencyContacts: [],
};
const memberFields = (email, roles, extra = {}) =>
  ({ email, displayName: email, approved: true, roles, hasAccount: true, ...fullProfile, ...extra });
const memberDoc = (email, roles, extra = {}) => fields(memberFields(email, roles, extra));
// Alle Zeitfelder einer Versammlung; die Rules verlangen sie vollständig.
const fullMeeting = {
  midweek_meeting_time: '19:00', midweek_meeting_weekday: 3, weekend_meeting_time: '17:30', weekend_meeting_weekday: 7,
  yearly_meeting_times: [],
};

// ---------- Nutzer und Daten (Seeding mit dem Owner-Token umgeht die Rules) ----------
const admin = await signUp('admin@test.de');
const planner = await signUp('planner@test.de');
const taskPlanner = await signUp('taskplanner@test.de');
const plain = await signUp('plain@test.de');

const owner = 'owner';
await api('PATCH', `admins/${admin.uid}`, { fields: {} }, owner);
await api('PATCH', `${CONG}/members/${planner.uid}`, memberDoc('planner@test.de', ['TALK_PLANNER']), owner);
await api('PATCH', `${CONG}/members/${taskPlanner.uid}`, memberDoc('taskplanner@test.de', ['TASK_PLANNER']), owner);
await api('PATCH', `${CONG}/members/${plain.uid}`, memberDoc('plain@test.de', []), owner);
const congAdmin = await signUp('congadmin@test.de');
await api('PATCH', `${CONG}/members/${congAdmin.uid}`, memberDoc('congadmin@test.de', ['CONGREGATION_ADMIN'], { displayName: 'ca' }), owner);
const adminPlanner = await signUp('adminplanner@test.de');
await api('PATCH', `${CONG}/members/${adminPlanner.uid}`, memberDoc('adminplanner@test.de', ['CONGREGATION_ADMIN', 'TALK_PLANNER'], { displayName: 'ap' }), owner);
// Ein eigener Admin für die Tests, in denen ein Admin die eigene Rollenliste ändert.
const selfAdmin = await signUp('selfadmin@test.de');
await api('PATCH', `${CONG}/members/${selfAdmin.uid}`, memberDoc('selfadmin@test.de', ['CONGREGATION_ADMIN']), owner);
await api('PATCH', 'region/R/subregion/S/branch/B/country/L/circuit/C/uniqueCongregationNames/berlin', fields({ congregationId: 'K' }), owner);
const OTHER_CONG ='region/R/subregion/S/branch/B/country/L/circuit/C/congregation/OTHER';
// Die Kreise müssen existieren: eine Versammlung entsteht nur in einem vorhandenen Kreis.
await api('PATCH', CIRCUIT, fields({ name: 'Kreis C' }), owner);
await api('PATCH', 'region/R/subregion/S/branch/B/country/L/circuit/C2', fields({ name: 'Kreis C2' }), owner);
await api('PATCH', CONG, fields({ name: 'Berlin', ...fullMeeting }), owner);
await api('PATCH', OTHER_CONG, fields({ name: 'Hamburg', ...fullMeeting }), owner);

const SLOT = `${CONG}/talkSchedule`;
const base = { time: '17:30', talkCancelled: false, confirmed: false };
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
await expect('Datum 2026-02-31 gibt es nicht', putSlot('2026-02-31', base), false);
await expect('Datum 2026-04-31 gibt es nicht', putSlot('2026-04-31', base), false);
await expect('Datum 2026-02-30 gibt es nicht', putSlot('2026-02-30', base), false);
await expect('Schalttag 2028-02-29', putSlot('2028-02-29', base), true);
await expect('Jahresende 2026-12-31', putSlot('2026-12-31', base), true);
await expect('Termin ohne Bestätigungsfeld', putSlot('2026-10-25', { time: '17:30', talkCancelled: false }), false);
await expect('Vortragsnummer 9999', putSlot('2026-10-25', { ...base, talkNumber: 9999, talkTitle: 'x' }), true);
await expect('Vortragsnummer 10000', putSlot('2026-10-25', { ...base, talkNumber: 10000, talkTitle: 'x' }), false);
await expect('unbekanntes Feld', putSlot('2026-10-25', { ...base, extra: 'x' }), false);
await expect('Termin bestätigt', putSlot('2026-10-25', { ...base, confirmed: true }), true);
await expect('Termin nicht bestätigt', putSlot('2026-10-25', { ...base, confirmed: false }), true);
await expect('Bestätigung kein Bool', putSlot('2026-10-25', { ...base, confirmed: 'ja' }), false);
await expect('Notiz gültig', putSlot('2026-10-25', { ...base, note: 'Redner bringt Beamer mit' }), true);
await expect('Notiz 500 Zeichen', putSlot('2026-10-25', { ...base, note: 'x'.repeat(500) }), true);
await expect('Notiz 501 Zeichen', putSlot('2026-10-25', { ...base, note: 'x'.repeat(501) }), false);
await expect('Notiz kein String', putSlot('2026-10-25', { ...base, note: 5 }), false);
await expect('ungültige Uhrzeit', putSlot('2026-10-25', { time: '25:00', talkCancelled: false, confirmed: false }), false);
await expect('Planer liest Termin', api('GET', `${SLOT}/2026-10-25`, undefined, planner.token), true);
await expect('Nutzer ohne Planerrolle schreibt Termin', putSlot('2026-11-01', base, plain.token), false);
await expect('Nutzer ohne Planerrolle liest Termin', api('GET', `${SLOT}/2026-10-25`, undefined, plain.token), false);
await expect('Planer löscht Termin', api('DELETE', `${SLOT}/2026-10-25`, undefined, planner.token), true);

// ---------- Verzeichnis externer Redner (Kreis → Versammlung → Redner) ----------
const EXT_CIRCUITS = `${CONG}/externalCircuits`;
const EXT_CONGREGATIONS = `${CONG}/externalCongregations`;
const EXT_SPEAKERS = `${CONG}/visitingSpeakers`;
const post = (path, data, token = planner.token) => call('POST', `${FS}/${path}`, fields(data), token);
const validCircuit = { name: 'Kreis Nord' };
const validCongregation = { circuitId: 'c1', name: 'Musterstadt' };
const validSpeaker = {
  congregationId: 'g1',
  lastName: 'Extern',
  firstName: 'Eva',
  mobilePhone: '0170 1234',
  landlinePhone: '08092 1234',
  email: 'eva@example.org',
  talkNumbers: [12, 45],
  ranking: 3,
};
await expect('externen Kreis anlegen', post(EXT_CIRCUITS, validCircuit), true);
await expect('externer Kreis ohne Namen', post(EXT_CIRCUITS, { name: '' }), false);
await expect('externer Kreis mit Zusatzfeld', post(EXT_CIRCUITS, { ...validCircuit, x: 'y' }), false);
await expect('externe Versammlung anlegen', post(EXT_CONGREGATIONS, validCongregation), true);
await expect('externe Versammlung ohne Kreis', post(EXT_CONGREGATIONS, { name: 'Musterstadt' }), false);
await expect('externen Redner anlegen', post(EXT_SPEAKERS, validSpeaker), true);
await expect('externer Redner ohne Vortragsliste', post(EXT_SPEAKERS, { ...validSpeaker, talkNumbers: [] }), true);
await expect('externer Redner ohne Versammlung', post(EXT_SPEAKERS, { ...validSpeaker, congregationId: '' }), false);
await expect('externer Redner mit leerem Familiennamen', post(EXT_SPEAKERS, { ...validSpeaker, lastName: '' }), false);
await expect('externer Redner mit Vortragsliste als Text', post(EXT_SPEAKERS, { ...validSpeaker, talkNumbers: '12' }), false);
await expect('externer Redner mit zu langer Handynummer', post(EXT_SPEAKERS, { ...validSpeaker, mobilePhone: '1'.repeat(31) }), false);
await expect('externer Redner mit zu langer Festnetznummer', post(EXT_SPEAKERS, { ...validSpeaker, landlinePhone: '1'.repeat(31) }), false);
await expect('externer Redner mit Bewertung 5', post(EXT_SPEAKERS, { ...validSpeaker, ranking: 5 }), true);
await expect('externer Redner mit Bewertung 6', post(EXT_SPEAKERS, { ...validSpeaker, ranking: 6 }), false);
await expect('externer Redner mit negativer Bewertung', post(EXT_SPEAKERS, { ...validSpeaker, ranking: -1 }), false);
await expect('externer Redner mit Bewertung als Text', post(EXT_SPEAKERS, { ...validSpeaker, ranking: '3' }), false);
await expect('externer Redner ohne Bewertung', post(EXT_SPEAKERS, Object.fromEntries(Object.entries(validSpeaker).filter(([key]) => key !== 'ranking'))), false);
await expect('externer Redner mit Zusatzfeld', post(EXT_SPEAKERS, { ...validSpeaker, x: 'y' }), false);
await expect('externes Verzeichnis lesen als Planer', api('GET', EXT_SPEAKERS, undefined, planner.token), true);
await expect('externe Kreise lesen ohne Planerrolle', api('GET', EXT_CIRCUITS, undefined, plain.token), false);
await expect('externe Redner lesen ohne Planerrolle', api('GET', EXT_SPEAKERS, undefined, plain.token), false);
await expect('externen Redner anlegen ohne Planerrolle', post(EXT_SPEAKERS, validSpeaker, plain.token), false);

// ---------- Vortragshistorie (CP-139: aus den Terminen berechnet, keine eigene Collection mehr) ----------
const HIST = `${CONG}/talkHistory`;
await expect('Planer schreibt keine talkHistory mehr', api('PATCH', `${HIST}/7`, fields({ heldOn: ['2026-03-01'] }), planner.token), false);
await expect('Planer liest keine talkHistory mehr', api('GET', HIST, undefined, planner.token), false);

// ---------- Mitglieder lesen (Planerrollen) ----------
const MEMBERS = `${CONG}/members`;
await expect('Vortragsplaner listet Mitglieder', api('GET', MEMBERS, undefined, planner.token), true);
await expect('Vortragsplaner liest das eigene Mitglied', api('GET', `${MEMBERS}/${planner.uid}`, undefined, planner.token), true);
await expect('Vortragsplaner liest ein fremdes Mitglied', api('GET', `${MEMBERS}/${plain.uid}`, undefined, planner.token), true);
await expect('Aufgabenplaner listet Mitglieder', api('GET', MEMBERS, undefined, taskPlanner.token), true);
await expect('Aufgabenplaner liest ein fremdes Mitglied', api('GET', `${MEMBERS}/${plain.uid}`, undefined, taskPlanner.token), true);
await expect(
  'Vortragsplaner schreibt ein Mitglied nicht',
  api('PATCH', `${MEMBERS}/${plain.uid}?updateMask.fieldPaths=approved`, { fields: { approved: { booleanValue: false } } }, planner.token),
  false,
);
await expect(
  'Aufgabenplaner schreibt ein Mitglied nicht',
  api('PATCH', `${MEMBERS}/${plain.uid}?updateMask.fieldPaths=approved`, { fields: { approved: { booleanValue: false } } }, taskPlanner.token),
  false,
);
await expect('Mitglied ohne Rolle listet Mitglieder', api('GET', MEMBERS, undefined, plain.token), false);
await expect('Mitglied ohne Rolle liest ein fremdes Mitglied', api('GET', `${MEMBERS}/${planner.uid}`, undefined, plain.token), false);
await expect('Global Admin listet Mitglieder nicht', api('GET', MEMBERS, undefined, admin.token), false);

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

// ---------- Versammlungs-Metadaten (Versammlungsadmin nur für die eigene Versammlung) ----------
const mask = (name) => `?updateMask.fieldPaths=${name}`;
const time = (value) => ({ fields: { midweek_meeting_time: { stringValue: value } } });
await expect('Versammlungsadmin ändert Zeiten der eigenen Versammlung', api('PATCH', `${CONG}${mask('midweek_meeting_time')}`, time('18:30'), congAdmin.token), true);
const weekdayBody = (value) => ({ fields: { midweek_meeting_weekday: { integerValue: String(value) } } });
await expect('Versammlungsadmin setzt ungültige Uhrzeit', api('PATCH', `${CONG}${mask('midweek_meeting_time')}`, time('25:00'), congAdmin.token), false);
await expect('Versammlungsadmin setzt Uhrzeit als Zahl', api('PATCH', `${CONG}${mask('midweek_meeting_time')}`, { fields: { midweek_meeting_time: { integerValue: '19' } } }, congAdmin.token), false);
await expect('Versammlungsadmin setzt Wochentag 7', api('PATCH', `${CONG}${mask('midweek_meeting_weekday')}`, weekdayBody(7), congAdmin.token), true);
await expect('Versammlungsadmin setzt Wochentag 0', api('PATCH', `${CONG}${mask('midweek_meeting_weekday')}`, weekdayBody(0), congAdmin.token), false);
await expect('Versammlungsadmin setzt Wochentag 8', api('PATCH', `${CONG}${mask('midweek_meeting_weekday')}`, weekdayBody(8), congAdmin.token), false);
await expect('Versammlungsadmin setzt Jahreszeiten als Text', api('PATCH', `${CONG}${mask('yearly_meeting_times')}`, { fields: { yearly_meeting_times: { stringValue: 'x' } } }, congAdmin.token), false);
await expect('Versammlungsadmin setzt Jahreszeiten', api('PATCH', `${CONG}${mask('yearly_meeting_times')}`, { fields: { yearly_meeting_times: { arrayValue: { values: [{ stringValue: '2026|3|19:00|7|17:30' }] } } } }, congAdmin.token), true);
await expect('Versammlungsadmin setzt zu viele Jahreszeiten', api('PATCH', `${CONG}${mask('yearly_meeting_times')}`, { fields: { yearly_meeting_times: { arrayValue: { values: Array.from({ length: 101 }, () => ({ stringValue: '2026|3|19:00|7|17:30' })) } } } }, congAdmin.token), false);
await expect('Versammlungsadmin ändert fremdes Feld der eigenen Versammlung', api('PATCH', `${CONG}${mask('secret')}`, { fields: { secret: { stringValue: 'x' } } }, congAdmin.token), false);
await expect('Versammlungsadmin ändert Zeiten einer fremden Versammlung', api('PATCH', `${OTHER_CONG}${mask('midweek_meeting_time')}`, time('18:30'), congAdmin.token), false);
await expect('Versammlungsadmin löscht die eigene Versammlung', api('DELETE', CONG, undefined, congAdmin.token), false);
await expect('Planer ändert Zeiten der Versammlung', api('PATCH', `${CONG}${mask('midweek_meeting_time')}`, time('18:30'), planner.token), false);

// ---------- Admin ändert das eigene Mitglied: Rollenliste ja (aber nie ohne Admin-Rolle), Freigabe nein ----------
const rolesMask = (uid) => `${CONG}/members/${uid}?updateMask.fieldPaths=roles`;
const rolesBody = (...roles) => ({ fields: { roles: { arrayValue: { values: roles.map((stringValue) => ({ stringValue })) } } } });
await expect('Admin ergänzt die eigene Planerrolle', api('PATCH', rolesMask(selfAdmin.uid), rolesBody('CONGREGATION_ADMIN', 'TALK_PLANNER'), selfAdmin.token), true);
await expect('Admin ergänzt die eigene Aufgabenplaner-Rolle', api('PATCH', rolesMask(selfAdmin.uid), rolesBody('CONGREGATION_ADMIN', 'TALK_PLANNER', 'TASK_PLANNER'), selfAdmin.token), true);
await expect('Admin entfernt die eigene Planerrolle', api('PATCH', rolesMask(selfAdmin.uid), rolesBody('CONGREGATION_ADMIN'), selfAdmin.token), true);
await expect('Admin entfernt die eigene Admin-Rolle', api('PATCH', rolesMask(selfAdmin.uid), rolesBody('TALK_PLANNER'), selfAdmin.token), false);
await expect('Admin leert die eigene Rollenliste', api('PATCH', rolesMask(selfAdmin.uid), rolesBody(), selfAdmin.token), false);
await expect('Admin ändert die eigene Freigabe', api('PATCH', `${CONG}/members/${selfAdmin.uid}?updateMask.fieldPaths=approved`, { fields: { approved: { booleanValue: false } } }, selfAdmin.token), false);
await expect('Rollenliste mit unbekannter Rolle', api('PATCH', rolesMask(selfAdmin.uid), rolesBody('CONGREGATION_ADMIN', 'BOSS'), selfAdmin.token), false);
await expect('Rollenliste mit doppelter Rolle', api('PATCH', rolesMask(selfAdmin.uid), rolesBody('CONGREGATION_ADMIN', 'CONGREGATION_ADMIN'), selfAdmin.token), false);
await expect('Admin vergibt einem Mitglied Rollen', api('PATCH', rolesMask(plain.uid), rolesBody('TASK_PLANNER'), congAdmin.token), true);
await expect('Admin nimmt einem Mitglied die Rollen wieder', api('PATCH', rolesMask(plain.uid), rolesBody(), congAdmin.token), true);
await expect('Admin nimmt einem anderen Admin die Admin-Rolle', api('PATCH', rolesMask(adminPlanner.uid), rolesBody('TALK_PLANNER'), congAdmin.token), true);
await api('PATCH', rolesMask(adminPlanner.uid), rolesBody('CONGREGATION_ADMIN', 'TALK_PLANNER'), owner);
await expect('Vortragsplaner vergibt sich eine Rolle', api('PATCH', rolesMask(planner.uid), rolesBody('TALK_PLANNER', 'CONGREGATION_ADMIN'), planner.token), false);
await expect('Mitglied ohne Rolle vergibt sich eine Rolle', api('PATCH', rolesMask(plain.uid), rolesBody('TASK_PLANNER'), plain.token), false);

// ---------- Eigener Name: jedes Mitglied darf nur Vor- und Nachnamen im eigenen Dokument ändern ----------
const ownName = (uid, ...names) => `${CONG}/members/${uid}?${names.map((n) => `updateMask.fieldPaths=${n}`).join('&')}`;
const nameBody = (lastName, firstName) => ({ fields: { lastName: { stringValue: lastName }, firstName: { stringValue: firstName } } });
await expect('Mitglied ändert den eigenen Namen', api('PATCH', ownName(plain.uid, 'lastName', 'firstName'), nameBody('Muster', 'Max'), plain.token), true);
await expect('Mitglied ändert nur den eigenen Nachnamen', api('PATCH', ownName(plain.uid, 'lastName'), { fields: { lastName: { stringValue: 'Neu' } } }, plain.token), true);
await expect('Mitglied mit leerem Namen', api('PATCH', ownName(plain.uid, 'lastName', 'firstName'), nameBody('', ''), plain.token), true);
await expect('Mitglied mit zu langem Namen', api('PATCH', ownName(plain.uid, 'lastName', 'firstName'), nameBody('x'.repeat(101), 'Max'), plain.token), false);
await expect('Mitglied ändert den Namen eines anderen', api('PATCH', ownName(planner.uid, 'lastName', 'firstName'), nameBody('Fremd', 'Name'), plain.token), false);
await expect('Mitglied setzt zum Namen die Kategorie', api('PATCH', ownName(plain.uid, 'lastName', 'category'), { fields: { lastName: { stringValue: 'Muster' }, category: { stringValue: 'BROTHER' } } }, plain.token), false);
await expect('Mitglied setzt Namen und Telefonnummer zusammen', api('PATCH', ownName(plain.uid, 'lastName', 'mobilePhone'), { fields: { lastName: { stringValue: 'Muster' }, mobilePhone: { stringValue: '0171' } } }, plain.token), true);
await expect('Mitglied setzt sich als Redner', api('PATCH', ownName(plain.uid, 'isSpeaker'), { fields: { isSpeaker: { booleanValue: true } } }, plain.token), false);
await expect('Vortragsplaner ändert den eigenen Namen', api('PATCH', ownName(planner.uid, 'lastName', 'firstName'), nameBody('Planer', 'Paul'), planner.token), true);
await expect('Admin ändert den eigenen Namen', api('PATCH', ownName(adminPlanner.uid, 'lastName', 'firstName'), nameBody('Admin', 'Anna'), adminPlanner.token), true);

// ---------- Eigene Telefonnummern im Mitgliedsdokument ----------
await expect('Mitglied ändert die eigenen Telefonnummern', api('PATCH', ownName(plain.uid, 'mobilePhone', 'landlinePhone'), { fields: { mobilePhone: { stringValue: '0170 123456' }, landlinePhone: { stringValue: '+49 (30) 1234-56' } } }, plain.token), true);
await expect('Mitglied mit ungültiger Telefonnummer', api('PATCH', ownName(plain.uid, 'mobilePhone'), { fields: { mobilePhone: { stringValue: '0170-abc' } } }, plain.token), false);
await expect('Mitglied ändert die Telefonnummer eines anderen', api('PATCH', ownName(planner.uid, 'mobilePhone'), { fields: { mobilePhone: { stringValue: '0170' } } }, plain.token), false);

// ---------- Eigene zusätzliche E-Mail-Adressen im Mitgliedsdokument ----------
const emailsBody = (...emails) => ({ fields: { emails: { arrayValue: { values: emails.map((stringValue) => ({ stringValue })) } } } });
await expect('Mitglied speichert drei E-Mail-Adressen', api('PATCH', ownName(plain.uid, 'emails'), emailsBody('a@b.de', 'c.d@e-f.org', 'x+y@z.com'), plain.token), true);
await expect('Mitglied speichert vier E-Mail-Adressen', api('PATCH', ownName(plain.uid, 'emails'), emailsBody('a@b.de', 'c@d.de', 'e@f.de', 'g@h.de'), plain.token), false);
await expect('Mitglied speichert ungültige E-Mail-Adresse', api('PATCH', ownName(plain.uid, 'emails'), emailsBody('keine-adresse'), plain.token), false);
await expect('Mitglied ändert die E-Mail-Adressen eines anderen', api('PATCH', ownName(planner.uid, 'emails'), emailsBody('a@b.de'), plain.token), false);
await expect('Mitglied ändert die Anmelde-E-Mail', api('PATCH', ownName(plain.uid, 'email'), { fields: { email: { stringValue: 'neu@b.de' } } }, plain.token), false);

// ---------- Adresse und Notfallkontakte im Mitgliedsdokument (ADR-0016) ----------
const ADDRESS_FIELDS = ['street', 'postalCode', 'city', 'emergencyContacts'];
const phonesValue = (phones) => ({ arrayValue: { values: phones.map((stringValue) => ({ stringValue })) } });
const emergency = (name, phone, relation, more = []) => ({ mapValue: { fields: { name: { stringValue: name }, phones: phonesValue([phone, ...more]), ...(relation === undefined ? {} : { relation: { stringValue: relation } }) } } });
const addressBody = (contacts, extra = {}) => ({ fields: { street: { stringValue: 'Hauptstr. 1' }, postalCode: { stringValue: '10115' }, city: { stringValue: 'Berlin' }, emergencyContacts: { arrayValue: { values: contacts } }, ...extra } });
const putAddress = (uid, body, token, fields = ADDRESS_FIELDS) => api('PATCH', ownName(uid, ...fields), body, token);
await expect('Mitglied speichert eigene Adresse und Notfallkontakte', putAddress(plain.uid, addressBody([emergency('Anna Muster', '0170 1', 'Ehefrau'), emergency('Ben Muster', '030 2')]), plain.token), true);
// Worst case for the limit of 1000 expressions per request: everything at its maximum.
const fullContacts = [1, 2, 3, 4, 5].map((i) => emergency(`Kontakt ${i}`, `0170 ${i}`, 'Ehefrau', [`030 ${i}`, `0201 ${i}`]));
const fullExtras = { emails: { arrayValue: { values: ['a@b.de', 'c@d.de', 'e@f.de'].map((stringValue) => ({ stringValue })) } }, mobilePhone: { stringValue: '0170 123456' }, landlinePhone: { stringValue: '030 123456' } };
const fullFields = [...ADDRESS_FIELDS, 'emails', 'mobilePhone', 'landlinePhone'];
await expect('Mitglied speichert fünf Notfallkontakte mit je drei Nummern', putAddress(plain.uid, addressBody(fullContacts, fullExtras), plain.token, fullFields), true);
await expect('Admin speichert fünf Notfallkontakte mit je drei Nummern', putAddress(plain.uid, addressBody(fullContacts, fullExtras), congAdmin.token, fullFields), true);
await expect('Mitglied speichert sechs Notfallkontakte', putAddress(plain.uid, addressBody([1, 2, 3, 4, 5, 6].map((i) => emergency(`K${i}`, `0${i}`))), plain.token), false);
await expect('Notfallkontakt ohne Telefon', putAddress(plain.uid, addressBody([{ mapValue: { fields: { name: { stringValue: 'Anna' } } } }]), plain.token), false);
await expect('Notfallkontakt mit drei Telefonnummern', putAddress(plain.uid, addressBody([emergency('Anna', '0170 1', 'Ehefrau', ['030 2', '0201 3'])]), plain.token), true);
await expect('Notfallkontakt mit vier Telefonnummern', putAddress(plain.uid, addressBody([emergency('Anna', '0170 1', undefined, ['030 2', '0201 3', '089 4'])]), plain.token), false);
await expect('Notfallkontakt mit leerer Telefonliste', putAddress(plain.uid, addressBody([{ mapValue: { fields: { name: { stringValue: 'Anna' }, phones: phonesValue([]) } } }]), plain.token), false);
await expect('Notfallkontakt ohne Namen', putAddress(plain.uid, addressBody([emergency('', '0170')]), plain.token), false);
await expect('Notfallkontakt mit Zusatzfeld', putAddress(plain.uid, addressBody([{ mapValue: { fields: { name: { stringValue: 'Anna' }, phones: phonesValue(['0170']), email: { stringValue: 'a@b.de' } } } }]), plain.token), false);
await expect('Mitglied speichert ein unbekanntes Feld', putAddress(plain.uid, addressBody([], { iban: { stringValue: 'DE00' } }), plain.token, [...ADDRESS_FIELDS, 'iban']), false);
await expect('Adresse mit zu langer Postleitzahl', putAddress(plain.uid, addressBody([], { postalCode: { stringValue: '12345678901' } }), plain.token), false);
await expect('Mitglied ändert die Adresse eines anderen', putAddress(planner.uid, addressBody([]), plain.token), false);
await expect('Mitglied setzt sich zur Adresse als getauft', putAddress(plain.uid, addressBody([], { baptized: { booleanValue: true } }), plain.token, [...ADDRESS_FIELDS, 'baptized']), false);
await expect('Versammlungsadmin ändert Adresse und Notfallkontakte eines Mitglieds', putAddress(plain.uid, addressBody([emergency('Anna', '0170')]), congAdmin.token), true);
await expect('Vortragsplaner ändert die Adresse eines Mitglieds', putAddress(plain.uid, addressBody([]), planner.token), false);
await expect('Vortragsplaner liest ein Mitglied mit Adresse', api('GET', `${CONG}/members/${plain.uid}`, undefined, planner.token), true);
await expect('Mitglied ohne Rolle liest kein fremdes Mitglied', api('GET', `${CONG}/members/${planner.uid}`, undefined, plain.token), false);

// ---------- Planen nur als Vortragsplaner: die Rolle Versammlungsadmin allein genügt nicht ----------
await expect('Versammlungsadmin ohne Planerrolle schreibt Termin', putSlot('2026-11-08', base, congAdmin.token), false);
await expect('Versammlungsadmin ohne Planerrolle liest Termine', api('GET', SLOT, undefined, congAdmin.token), false);
await expect('Aufgabenplaner schreibt keinen Termin', putSlot('2026-11-08', base, taskPlanner.token), false);
await expect('Versammlungsadmin mit Vortragsplaner-Rolle schreibt Termin', putSlot('2026-11-08', base, adminPlanner.token), true);

// ---------- Selbstregistrierung: ohne Rollen und ohne die veralteten Felder ----------
const registerFields = (email, extra = {}) => ({ email, displayName: 'n', approved: false, roles: [], hasAccount: true, ...fullProfile, ...extra });
const registerBody = (email, extra = {}) => fields(registerFields(email, extra));
const newcomer = await signUp('newcomer@test.de');
const newcomer2 = await signUp('newcomer2@test.de');
const newcomer3 = await signUp('newcomer3@test.de');
const newcomer5 = await signUp('newcomer5@test.de');
const newcomer6 = await signUp('newcomer6@test.de');
const newcomer7 = await signUp('newcomer7@test.de');
const registerPath = (user) => `${CONG}/members/${user.uid}`;
await expect('Selbstregistrierung mit roles []', api('PATCH', registerPath(newcomer), registerBody('newcomer@test.de'), newcomer.token), true);
await expect('Selbstregistrierung mit Rolle', api('PATCH', registerPath(newcomer2), registerBody('newcomer2@test.de', { roles: ['CONGREGATION_ADMIN'] }), newcomer2.token), false);
await expect('Selbstregistrierung freigegeben', api('PATCH', registerPath(newcomer3), registerBody('newcomer3@test.de', { approved: true }), newcomer3.token), false);
await expect('Selbstregistrierung ohne roles', api('PATCH', registerPath(newcomer5), fields({ email: 'newcomer5@test.de', displayName: 'n', approved: false }), newcomer5.token), false);
await expect('Selbstregistrierung mit Vor- und Nachname', api('PATCH', registerPath(newcomer6), registerBody('newcomer6@test.de', { firstName: 'Max', lastName: 'Muster' }), newcomer6.token), true);
await expect('Selbstregistrierung mit Namen und anderen Profilangaben', api('PATCH', registerPath(newcomer7), registerBody('newcomer7@test.de', { firstName: 'Max', lastName: 'Muster', baptized: true }), newcomer7.token), false);
await expect('Selbstregistrierung mit fremder E-Mail-Adresse', api('PATCH', registerPath(newcomer5), registerBody('other@test.de'), newcomer5.token), false);


// ---------- Admin-Kontakte: Namen der Admins, auch vor der Freigabe lesbar ----------
const pending = await signUp('pending@test.de');
await api('PATCH', `${CONG}/members/${pending.uid}`, memberDoc('pending@test.de', [], { displayName: 'p', approved: false }), owner);
const outsider = await signUp('outsider@test.de');
const namedAdmin = await signUp('named@test.de');
await api('PATCH', `${CONG}/members/${namedAdmin.uid}`, memberDoc('named@test.de', ['CONGREGATION_ADMIN'], { displayName: 'n', firstName: 'Anna', lastName: 'Admin' }), owner);
const CONTACTS = `${CONG}/adminContacts`;
const contact = (firstName, lastName) => fields({ firstName, lastName });
const putContact = (uid, data, token) => api('PATCH', `${CONTACTS}/${uid}`, data, token);
await expect('Admin-Kontakt passend zum Admin anlegen', putContact(namedAdmin.uid, contact('Anna', 'Admin'), namedAdmin.token), true);
await expect('Admin-Kontakt mit falschem Namen', putContact(namedAdmin.uid, contact('Anna', 'Andere'), namedAdmin.token), false);
await expect('Admin-Kontakt mit Zusatzfeld E-Mail', putContact(namedAdmin.uid, fields({ firstName: 'Anna', lastName: 'Admin', email: 'named@test.de' }), namedAdmin.token), false);
await expect('Admin-Kontakt für ein Mitglied, das kein Admin ist', putContact(plain.uid, contact('', ''), congAdmin.token), false);
await expect('Admin-Kontakt für einen Admin im neuen Format', putContact(selfAdmin.uid, contact('', ''), congAdmin.token), true);
await expect('Admin-Kontakt für eine nicht freigegebene Person', putContact(pending.uid, contact('', ''), congAdmin.token), false);
await expect('Admin-Kontakt ohne Namen eines Admins ohne Namen', putContact(congAdmin.uid, contact('', ''), congAdmin.token), true);
await expect('Admin-Kontakt von einem anderen Versammlungsadmin pflegen', putContact(namedAdmin.uid, contact('Anna', 'Admin'), congAdmin.token), true);
await expect('Admin-Kontakt von Global Admin pflegen', putContact(namedAdmin.uid, contact('Anna', 'Admin'), admin.token), false);
await expect('Admin-Kontakt schreiben ohne Freigabe', putContact(namedAdmin.uid, contact('Anna', 'Admin'), pending.token), false);
await expect('Admin-Kontakt schreiben als normales Mitglied', putContact(namedAdmin.uid, contact('Anna', 'Admin'), plain.token), false);
await expect('Admin-Kontakt lesen ohne Freigabe', api('GET', `${CONTACTS}/${namedAdmin.uid}`, undefined, pending.token), true);
await expect('Admin-Kontakte auflisten ohne Freigabe', api('GET', CONTACTS, undefined, pending.token), true);
await expect('Admin-Kontakt lesen als Freigegebener', api('GET', `${CONTACTS}/${namedAdmin.uid}`, undefined, plain.token), true);
await expect('Admin-Kontakt lesen ohne Mitglied der Versammlung', api('GET', `${CONTACTS}/${namedAdmin.uid}`, undefined, outsider.token), false);
await expect('Admin-Kontakt lesen ohne Anmeldung', api('GET', `${CONTACTS}/${namedAdmin.uid}`), false);
await expect('Admin-Kontakt lesen in einer fremden Versammlung', api('GET', `${OTHER_CONG}/adminContacts/${namedAdmin.uid}`, undefined, pending.token), false);
await expect('Nutzer ohne Freigabe liest weiterhin keine Mitglieder', api('GET', `${CONG}/members/${namedAdmin.uid}`, undefined, pending.token), false);
await expect('Nutzer ohne Freigabe listet keine Mitglieder', api('GET', `${CONG}/members`, undefined, pending.token), false);
await expect('Admin-Kontakt löschen ohne Freigabe', api('DELETE', `${CONTACTS}/${namedAdmin.uid}`, undefined, pending.token), false);
await expect('Admin-Kontakt eines anderen löschen ohne Freigabe', api('DELETE', `${CONTACTS}/${namedAdmin.uid}`, undefined, pending.token), false);
await expect('eigenen Admin-Kontakt löschen auch ohne Admin-Rolle', api('DELETE', `${CONTACTS}/${pending.uid}`, undefined, pending.token), true);
await expect('Admin-Kontakt löschen als Versammlungsadmin', api('DELETE', `${CONTACTS}/${namedAdmin.uid}`, undefined, congAdmin.token), true);


// ---------- Globaler Admin ohne Einblick in Versammlungsdaten (ADR-0013) ----------
const approvedMask = '?updateMask.fieldPaths=approved';
const falseApproved = { fields: { approved: { booleanValue: false } } };
await expect('Global Admin liest ein Mitglied nicht', api('GET', `${MEMBERS}/${plain.uid}`, undefined, admin.token), false);
await expect('Global Admin ändert ein Mitglied nicht', api('PATCH', `${MEMBERS}/${plain.uid}${approvedMask}`, falseApproved, admin.token), false);
await expect('Global Admin löscht ein Mitglied nicht', api('DELETE', `${MEMBERS}/${plain.uid}`, undefined, admin.token), false);
await expect(
  'Global Admin erfasst keine Person ohne Konto',
  call('POST', `${FS}/${MEMBERS}`, fields({ email: null, displayName: null, approved: false, roles: [], hasAccount: false, lastName: 'Muster', firstName: 'Lena' }), admin.token),
  false,
);
await expect('Global Admin plant keinen Termin', putSlot('2026-12-06', base, admin.token), false);
await expect('Global Admin liest keine Termine', api('GET', SLOT, undefined, admin.token), false);
await expect('Global Admin liest keine Vortragshistorie', api('GET', HIST, undefined, admin.token), false);
await expect('Global Admin liest kein Redner-Verzeichnis', api('GET', EXT_SPEAKERS, undefined, admin.token), false);
await expect('Global Admin legt keinen externen Redner an', post(EXT_SPEAKERS, validSpeaker, admin.token), false);
await expect('Global Admin liest weiter die Admin-Kontakte', api('GET', CONTACTS, undefined, admin.token), true);
await expect('Global Admin ändert den Namen der Versammlung nicht ohne Namensdokument', api('PATCH', `${CONG}${mask('name')}`, { fields: { name: { stringValue: 'Berlin-Mitte' } } }, admin.token), false);
await expect('Global Admin ändert den Namen der Versammlung samt Namensdokument', commit([
  { update: { name: `${DOCS}/${CONG}`, fields: { name: { stringValue: 'Berlin-Mitte' } } }, updateMask: { fieldPaths: ['name'] } },
  delDoc(`${CIRCUIT}/uniqueCongregationNames/berlin`),
  setDoc(`${CIRCUIT}/uniqueCongregationNames/berlin-mitte`, { congregationId: 'K' }),
], admin.token), true);
await expect('Global Admin ändert die Zeiten der Versammlung nicht', api('PATCH', `${CONG}${mask('midweek_meeting_time')}`, time('18:15'), admin.token), false);
await expect('Global Admin ändert ein fremdes Feld der Versammlung nicht', api('PATCH', `${CONG}${mask('secret')}`, { fields: { secret: { stringValue: 'x' } } }, admin.token), false);
const NEW_CONG = `${CIRCUIT}/congregation/NEW`;
const newCongregation = (name, uid, extra = {}) => fields({ name, createdBy: uid, ...fullMeeting, ...extra });
await expect('Global Admin legt eine Versammlung nicht ohne Namensdokument an', api('PATCH', NEW_CONG, newCongregation('Neu', admin.uid), admin.token), false);
await expect('Global Admin legt eine Versammlung mit Namensdokument an', commit([
  setDoc(NEW_CONG, { name: 'Neu', createdBy: admin.uid, ...fullMeeting }),
  setDoc(`${CIRCUIT}/uniqueCongregationNames/neu`, { congregationId: 'NEW' }),
], admin.token), true);
await expect('Global Admin löscht eine Versammlung nicht ohne ihr Namensdokument', api('DELETE', NEW_CONG, undefined, admin.token), false);
await expect('Global Admin löscht eine Versammlung mit ihrem Namensdokument', commit([
  delDoc(NEW_CONG),
  delDoc(`${CIRCUIT}/uniqueCongregationNames/neu`),
], admin.token), true);
await expect('Global Admin pflegt weiter die Kreise', api('PATCH', 'region/R2', fields({ name: 'Region 2' }), admin.token), true);
await expect('Normaler Nutzer pflegt keine Kreise', api('PATCH', 'region/R3', fields({ name: 'Region 3' }), plain.token), false);

// ---------- Organisationsbaum: nur der globale Admin, nur bekannte Felder (CP-72) ----------
const ORG_UNITS = ['region/OR', 'region/OR/subregion/OS', 'region/OR/subregion/OS/branch/OB', 'region/OR/subregion/OS/branch/OB/country/OL'];
for (const unit of ORG_UNITS) {
  const level = unit.split('/').at(-2);
  await expect(`Global Admin legt ${level} an`, api('PATCH', unit, fields({ name: 'Neu' }), admin.token), true);
  await expect(`Global Admin benennt ${level} um`, api('PATCH', unit, fields({ name: 'Umbenannt' }), admin.token), true);
  await expect(`${level} ohne Namen`, api('PATCH', unit, fields({}), admin.token), false);
  await expect(`${level} mit leerem Namen`, api('PATCH', unit, fields({ name: '' }), admin.token), false);
  await expect(`${level} mit zu langem Namen`, api('PATCH', unit, fields({ name: 'x'.repeat(101) }), admin.token), false);
  await expect(`${level} mit Zusatzfeld`, api('PATCH', unit, fields({ name: 'Neu', extra: 'x' }), admin.token), false);
  await expect(`Versammlungsadmin legt keine ${level} an`, api('PATCH', unit, fields({ name: 'Neu' }), congAdmin.token), false);
  await expect(`Gast legt keine ${level} an`, api('PATCH', unit, fields({ name: 'Neu' })), false);
  await expect(`Gast liest ${level}`, api('GET', unit), true);
}
const ORG_CIRCUIT = 'region/OR/subregion/OS/branch/OB/country/OL/circuit/OC';
await expect('Global Admin legt einen Kreis an', api('PATCH', ORG_CIRCUIT, fields({ name: 'Kreis 1', overseer_name: 'Max Muster' }), admin.token), true);
await expect('Global Admin legt einen Kreis ohne Kreisaufseher an', api('PATCH', ORG_CIRCUIT, fields({ name: 'Kreis 1' }), admin.token), true);
await expect('Kreis ohne Namen', api('PATCH', ORG_CIRCUIT, fields({ overseer_name: 'Max' }), admin.token), false);
await expect('Kreis mit zu langem Kreisaufseher', api('PATCH', ORG_CIRCUIT, fields({ name: 'Kreis 1', overseer_name: 'x'.repeat(101) }), admin.token), false);
await expect('Kreis mit Zusatzfeld', api('PATCH', ORG_CIRCUIT, fields({ name: 'Kreis 1', number: '609a' }), admin.token), false);
await expect('Versammlungsadmin legt keinen Kreis an', api('PATCH', ORG_CIRCUIT, fields({ name: 'Kreis 1' }), congAdmin.token), false);
await expect('Versammlungsadmin löscht keinen Kreis', api('DELETE', ORG_CIRCUIT, undefined, congAdmin.token), false);
await expect('Versammlungsadmin löscht keine Region', api('DELETE', 'region/OR', undefined, congAdmin.token), false);
await expect('Global Admin löscht einen Kreis', api('DELETE', ORG_CIRCUIT, undefined, admin.token), true);
for (const unit of [...ORG_UNITS].reverse()) {
  await expect(`Global Admin löscht ${unit.split('/').at(-2)}`, api('DELETE', unit, undefined, admin.token), true);
}

// Doppelrolle: ein globaler Admin, der zugleich Vortragsplaner einer Versammlung ist, plant über seine Rolle.
const dualAdmin = await signUp('dualadmin@test.de');
await api('PATCH', `admins/${dualAdmin.uid}`, { fields: {} }, owner);
await api('PATCH', `${MEMBERS}/${dualAdmin.uid}`, memberDoc('dualadmin@test.de', ['TALK_PLANNER']), owner);
await expect('Global Admin mit Planerrolle plant über die Rolle', putSlot('2026-12-13', base, dualAdmin.token), true);
await expect('Global Admin mit Planerrolle liest Mitglieder über die Rolle', api('GET', MEMBERS, undefined, dualAdmin.token), true);
await expect('Global Admin mit Planerrolle ändert trotzdem kein Mitglied', api('PATCH', `${MEMBERS}/${plain.uid}${approvedMask}`, falseApproved, dualAdmin.token), false);
await expect('Global Admin mit Planerrolle pflegt weiter die Vortragsliste', api('PATCH', 'talkCatalog/77', talk('Dual'), dualAdmin.token), true);


// ---------- Versammlung anlegen: der Ersteller wird Admin, der Name ist pro Kreis eindeutig ----------
const CIRCUIT_2 = 'region/R/subregion/S/branch/B/country/L/circuit/C2';
const creator = await signUpVerified('creator@test.de');
const unverified = await signUp('unverified@test.de');
const intruder = await signUpVerified('intruder@test.de');
const adminMember = (user, email, extra = {}) => ({
  ...fullProfile, email, displayName: 'Max Muster', approved: true, roles: ['CONGREGATION_ADMIN'], firstName: 'Max', lastName: 'Muster', hasAccount: true, ...extra,
});
const createBatch = (circuit, congId, name, key, user, email, memberExtra = {}, congExtra = {}) => [
  setDoc(`${circuit}/congregation/${congId}`, { name, createdBy: user.uid, ...fullMeeting, ...congExtra }),
  setDoc(`${circuit}/uniqueCongregationNames/${key}`, { congregationId: congId }),
  setDoc(`${circuit}/congregation/${congId}/members/${user.uid}`, adminMember(user, email, memberExtra)),
];

await expect('Neue Versammlung mit Namensdokument und Admin-Mitglied', commit(createBatch(CIRCUIT, 'GRAFING', 'Grafing Süd', 'grafing süd', creator, 'creator@test.de'), creator.token), true);
await expect('Der Ersteller ist danach freigegebener Admin', api('GET', `${CIRCUIT}/congregation/GRAFING/members/${creator.uid}`, undefined, creator.token), true);
await expect('Neue Versammlung ohne bestätigte E-Mail-Adresse', commit(createBatch(CIRCUIT, 'UNV', 'Unbestätigt', 'unbestätigt', unverified, 'unverified@test.de'), unverified.token), false);
await expect('Gleicher Name im selben Kreis (Groß-/Kleinschreibung und Leerzeichen egal)', commit(createBatch(CIRCUIT, 'DUP', ' GRAFING   süd ', 'grafing süd', intruder, 'intruder@test.de'), intruder.token), false);
await expect('Gleicher Name in einem anderen Kreis', commit(createBatch(CIRCUIT_2, 'GRAFING2', 'Grafing Süd', 'grafing süd', intruder, 'intruder@test.de'), intruder.token), true);
await expect('Versammlung ohne Namensdokument', commit([
  setDoc(`${CIRCUIT}/congregation/NONAME`, { name: 'Ohne Namensdokument', createdBy: intruder.uid, ...fullMeeting }),
], intruder.token), false);
await expect('Namensdokument mit falschem Schlüssel', commit(createBatch(CIRCUIT, 'WRONGKEY', 'Falscher Schlüssel', 'anderer name', intruder, 'intruder@test.de'), intruder.token), false);
await expect('Versammlung mit fremdem createdBy', commit(createBatch(CIRCUIT, 'FOREIGN', 'Fremd', 'fremd', intruder, 'intruder@test.de', {}, { createdBy: creator.uid }), intruder.token), false);
await expect('Versammlungsname mit Schrägstrich', commit(createBatch(CIRCUIT, 'SLASH', 'Nord/Süd', 'nord-süd', intruder, 'intruder@test.de'), intruder.token), false);
await expect('Ersteller mit zusätzlicher Rolle', commit(createBatch(CIRCUIT, 'ROLES', 'Mit Rollen', 'mit rollen', intruder, 'intruder@test.de', { roles: ['CONGREGATION_ADMIN', 'TALK_PLANNER'] }), intruder.token), false);
await expect('Ersteller ohne Admin-Rolle als freigegebenes Mitglied', commit(createBatch(CIRCUIT, 'PLAIN', 'Ohne Admin', 'ohne admin', intruder, 'intruder@test.de', { roles: [] }), intruder.token), false);
await expect('Fremder macht sich zum Admin einer bestehenden Versammlung', api('PATCH', `${CIRCUIT}/congregation/GRAFING/members/${intruder.uid}`, fields(adminMember(intruder, 'intruder@test.de')), intruder.token), false);
await expect('Admin-Mitglied einer bestehenden Versammlung auch im Batch mit einer anderen Neuanlage', commit([
  ...createBatch(CIRCUIT, 'SECOND', 'Zweite', 'zweite', intruder, 'intruder@test.de'),
  setDoc(`${CIRCUIT}/congregation/GRAFING/members/${intruder.uid}`, adminMember(intruder, 'intruder@test.de')),
], intruder.token), false);

const renamed = (name) => ({ update: { name: `${DOCS}/${CIRCUIT}/congregation/GRAFING`, fields: { name: { stringValue: name } } }, updateMask: { fieldPaths: ['name'] } });
await expect('Admin benennt um ohne neues Namensdokument', commit([renamed('Grafing Nord')], creator.token), false);
await expect('Admin benennt um und nimmt einen vergebenen Namen', commit([
  renamed('Berlin-Mitte'), delDoc(`${CIRCUIT}/uniqueCongregationNames/grafing süd`), setDoc(`${CIRCUIT}/uniqueCongregationNames/berlin-mitte`, { congregationId: 'GRAFING' }),
], creator.token), false);
await expect('Admin gibt den Namen frei ohne umzubenennen', commit([delDoc(`${CIRCUIT}/uniqueCongregationNames/grafing süd`)], creator.token), false);
await expect('Admin benennt um und führt das Namensdokument mit', commit([
  renamed('Grafing Nord'), delDoc(`${CIRCUIT}/uniqueCongregationNames/grafing süd`), setDoc(`${CIRCUIT}/uniqueCongregationNames/grafing nord`, { congregationId: 'GRAFING' }),
], creator.token), true);
await expect('Der alte Name ist nach dem Umbenennen wieder frei', commit(createBatch(CIRCUIT, 'AGAIN', 'Grafing Süd', 'grafing süd', intruder, 'intruder@test.de'), intruder.token), true);
await expect('Namensdokumente sind ohne Anmeldung lesbar', api('GET', `${CIRCUIT}/uniqueCongregationNames/${encodeURIComponent('grafing nord')}`), true);

// ---------- Härtung (CP-123): nur existierende Pfade, vollständige Dokumente, Admin bleibt Admin ----------
const GHOST_CIRCUIT = 'region/R/subregion/S/branch/B/country/L/circuit/GHOST';
await expect('Versammlung in einem nicht existierenden Kreis', commit(createBatch(GHOST_CIRCUIT, 'G1', 'Geist', 'geist', intruder, 'intruder@test.de'), intruder.token), false);
await expect('Global Admin legt keine Versammlung in einem nicht existierenden Kreis an', commit([
  setDoc(`${GHOST_CIRCUIT}/congregation/G2`, { name: 'Geist 2', createdBy: admin.uid, ...fullMeeting }),
  setDoc(`${GHOST_CIRCUIT}/uniqueCongregationNames/geist 2`, { congregationId: 'G2' }),
], admin.token), false);
await expect('Versammlung ohne Zeitfelder', commit([
  setDoc(`${CIRCUIT}/congregation/NOTIMES`, { name: 'Ohne Zeiten', createdBy: intruder.uid }),
  setDoc(`${CIRCUIT}/uniqueCongregationNames/ohne zeiten`, { congregationId: 'NOTIMES' }),
  setDoc(`${CIRCUIT}/congregation/NOTIMES/members/${intruder.uid}`, adminMember(intruder, 'intruder@test.de')),
], intruder.token), false);
await expect('Versammlung mit ungültiger Uhrzeit', commit(createBatch(CIRCUIT, 'BADTIME', 'Falsche Zeit', 'falsche zeit', intruder, 'intruder@test.de', {}, { weekend_meeting_time: '25:61' }), intruder.token), false);
await expect('Versammlung mit ungültigem Wochentag', commit(createBatch(CIRCUIT, 'BADDAY', 'Falscher Tag', 'falscher tag', intruder, 'intruder@test.de', {}, { weekend_meeting_weekday: 0 }), intruder.token), false);

const ghostNewcomer = await signUp('ghostnewcomer@test.de');
await expect('Selbstregistrierung in einer nicht existierenden Versammlung', api('PATCH', `${CIRCUIT}/congregation/NOPE/members/${ghostNewcomer.uid}`, registerBody('ghostnewcomer@test.de'), ghostNewcomer.token), false);
const incomplete = await signUp('incomplete@test.de');
await expect('Selbstregistrierung ohne Profilfelder', api('PATCH', registerPath(incomplete), fields({ email: 'incomplete@test.de', displayName: 'n', approved: false, roles: [], hasAccount: true }), incomplete.token), false);
const noAccountFlag = await signUp('noaccountflag@test.de');
const { hasAccount: _omitted, ...withoutHasAccount } = registerFields('noaccountflag@test.de');
await expect('Selbstregistrierung ohne hasAccount', api('PATCH', registerPath(noAccountFlag), fields(withoutHasAccount), noAccountFlag.token), false);
const noEmails = await signUp('noemails@test.de');
const { emails: _noEmails, ...withoutEmails } = registerFields('noemails@test.de');
await expect('Selbstregistrierung ohne emails', api('PATCH', registerPath(noEmails), fields(withoutEmails), noEmails.token), false);

// Personen ohne Konto: nur der Versammlungsadmin, vollständig und ohne Zugang.
const accountless = { email: null, displayName: null, hasAccount: false, approved: false, roles: [], ...fullProfile, lastName: 'Muster', firstName: 'Lena' };
await expect('Versammlungsadmin erfasst eine Person ohne Konto', call('POST', `${FS}/${MEMBERS}`, fields(accountless), congAdmin.token), true);
await expect('Person ohne Konto mit Freigabe', call('POST', `${FS}/${MEMBERS}`, fields({ ...accountless, approved: true }), congAdmin.token), false);
await expect('Person ohne Konto mit Rolle', call('POST', `${FS}/${MEMBERS}`, fields({ ...accountless, roles: ['TALK_PLANNER'] }), congAdmin.token), false);
await expect('Person ohne Konto ohne Namen', call('POST', `${FS}/${MEMBERS}`, fields({ ...accountless, lastName: '', firstName: '' }), congAdmin.token), false);
await expect('Vortragsplaner erfasst keine Person ohne Konto', call('POST', `${FS}/${MEMBERS}`, fields(accountless), planner.token), false);

// Löschen: ein Mitglied darf gehen, ein Admin nicht das eigene Dokument löschen (sonst bleibt die Versammlung ohne Admin).
const leaver = await signUp('leaver@test.de');
const spare = await signUp('spare@test.de');
const lastAdmin = await signUp('lastadmin@test.de');
await api('PATCH', `${MEMBERS}/${leaver.uid}`, memberDoc('leaver@test.de', []), owner);
await api('PATCH', `${MEMBERS}/${spare.uid}`, memberDoc('spare@test.de', []), owner);
await api('PATCH', `${MEMBERS}/${lastAdmin.uid}`, memberDoc('lastadmin@test.de', ['CONGREGATION_ADMIN']), owner);
await expect('Admin löscht das eigene Mitgliedsdokument nicht', api('DELETE', `${MEMBERS}/${lastAdmin.uid}`, undefined, lastAdmin.token), false);
await expect('Mitglied verlässt die Versammlung', api('DELETE', `${MEMBERS}/${leaver.uid}`, undefined, leaver.token), true);
await expect('Mitglied löscht ein fremdes Mitglied nicht', api('DELETE', `${MEMBERS}/${spare.uid}`, undefined, plain.token), false);
await expect('Admin löscht ein anderes Mitglied', api('DELETE', `${MEMBERS}/${spare.uid}`, undefined, congAdmin.token), true);
await expect('Admin löscht einen anderen Admin', api('DELETE', `${MEMBERS}/${lastAdmin.uid}`, undefined, congAdmin.token), true);

// ---------- Globaler Admin pflegt die eigenen Kontaktdaten im Admin-Dokument (CP-125) ----------
const ADMIN_DOC = `admins/${admin.uid}`;
const adminProfile = { lastName: 'Admin', firstName: 'Anna', mobilePhone: '0170 123456', landlinePhone: '', emails: ['a@b.de'] };
const putAdminProfile = (uid, data, token) => api('PATCH', `admins/${uid}`, fields(data), token);
const withoutKey = (data, key) => Object.fromEntries(Object.entries(data).filter(([name]) => name !== key));
await expect('Global Admin pflegt die eigenen Kontaktdaten', putAdminProfile(admin.uid, adminProfile, admin.token), true);
await expect('Global Admin ändert die Kontaktdaten erneut', putAdminProfile(admin.uid, { ...adminProfile, lastName: 'Neu', emails: [] }, admin.token), true);
await expect('Kontaktdaten ohne E-Mail-Liste', putAdminProfile(admin.uid, withoutKey(adminProfile, 'emails'), admin.token), false);
await expect('Kontaktdaten ohne Namen', putAdminProfile(admin.uid, withoutKey(adminProfile, 'lastName'), admin.token), false);
await expect('Kontaktdaten mit Zusatzfeld', putAdminProfile(admin.uid, { ...adminProfile, role: 'x' }, admin.token), false);
await expect('Kontaktdaten mit Adresse', putAdminProfile(admin.uid, { ...adminProfile, street: 'Hauptstr. 1' }, admin.token), false);
await expect('Kontaktdaten mit ungültiger Telefonnummer', putAdminProfile(admin.uid, { ...adminProfile, mobilePhone: '0170-abc' }, admin.token), false);
await expect('Kontaktdaten mit zu langem Namen', putAdminProfile(admin.uid, { ...adminProfile, firstName: 'x'.repeat(101) }, admin.token), false);
await expect('Kontaktdaten mit ungültiger E-Mail-Adresse', putAdminProfile(admin.uid, { ...adminProfile, emails: ['keine-adresse'] }, admin.token), false);
await expect('Kontaktdaten mit vier E-Mail-Adressen', putAdminProfile(admin.uid, { ...adminProfile, emails: ['a@b.de', 'c@d.de', 'e@f.de', 'g@h.de'] }, admin.token), false);
await expect('Kontaktdaten mit Name als Zahl', putAdminProfile(admin.uid, { ...adminProfile, lastName: 5 }, admin.token), false);
await expect('Anderer Nutzer ändert die Kontaktdaten eines globalen Admins', putAdminProfile(admin.uid, adminProfile, plain.token), false);
await expect('Vortragsplaner ändert die Kontaktdaten eines globalen Admins', putAdminProfile(admin.uid, adminProfile, planner.token), false);
await expect('Global Admin ändert die Kontaktdaten eines anderen globalen Admins', putAdminProfile(dualAdmin.uid, adminProfile, admin.token), false);
await expect('Nutzer legt kein Admin-Dokument für sich an', putAdminProfile(plain.uid, adminProfile, plain.token), false);
await expect('Global Admin löscht das eigene Admin-Dokument nicht', api('DELETE', ADMIN_DOC, undefined, admin.token), false);
await expect('Global Admin liest das eigene Admin-Dokument', api('GET', ADMIN_DOC, undefined, admin.token), true);
await expect('Anderer Nutzer liest ein Admin-Dokument nicht', api('GET', ADMIN_DOC, undefined, plain.token), false);
await expect('Admin-Dokumente lassen sich nicht auflisten', api('GET', 'admins', undefined, admin.token), false);

// ---------- Vortragsplanungs-Helfer (CP-127, ADR-0023) ----------
const helper = await signUp('helper@test.de');
const pendingHelper = await signUp('pendinghelper@test.de');
const roleTarget = await signUp('roletarget@test.de');
const helperNewcomer = await signUp('helpernewcomer@test.de');
await api('PATCH', `${MEMBERS}/${helper.uid}`, memberDoc('helper@test.de', ['TALK_HELPER']), owner);
await api('PATCH', `${MEMBERS}/${pendingHelper.uid}`, memberDoc('pendinghelper@test.de', ['TALK_HELPER'], { approved: false }), owner);
await api('PATCH', `${MEMBERS}/${roleTarget.uid}`, memberDoc('roletarget@test.de', []), owner);
const HELPER_SLOT = '2027-01-03';
await putSlot(HELPER_SLOT, { ...base, ...speaker });
const confirmedMask = `${SLOT}/${HELPER_SLOT}?updateMask.fieldPaths=confirmed`;
const confirmedBody = (value) => ({ fields: { confirmed: { booleanValue: value } } });
const slotMask = (...names) => `${SLOT}/${HELPER_SLOT}?${names.map((n) => `updateMask.fieldPaths=${n}`).join('&')}`;

await expect('Helfer liest einen Termin', api('GET', `${SLOT}/${HELPER_SLOT}`, undefined, helper.token), true);
await expect('Helfer listet die Termine', api('GET', SLOT, undefined, helper.token), true);
await expect('Helfer liest die externen Kreise', api('GET', EXT_CIRCUITS, undefined, helper.token), true);
await expect('Helfer liest die externen Versammlungen', api('GET', EXT_CONGREGATIONS, undefined, helper.token), true);
await expect('Helfer liest die externen Redner', api('GET', EXT_SPEAKERS, undefined, helper.token), true);
await expect('Helfer liest keine talkHistory', api('GET', HIST, undefined, helper.token), false);
await expect('Helfer listet die Mitglieder', api('GET', MEMBERS, undefined, helper.token), true);
await expect('Helfer liest ein fremdes Mitglied', api('GET', `${MEMBERS}/${planner.uid}`, undefined, helper.token), true);

await expect('Helfer markiert den Redner als informiert', api('PATCH', confirmedMask, confirmedBody(true), helper.token), true);
await expect('Helfer nimmt die Markierung zurück', api('PATCH', confirmedMask, confirmedBody(false), helper.token), true);
await expect('Markierung ohne Bool', api('PATCH', confirmedMask, { fields: { confirmed: { stringValue: 'ja' } } }, helper.token), false);
await expect('Helfer ändert die Notiz', api('PATCH', slotMask('note'), { fields: { note: { stringValue: 'x' } } }, helper.token), false);
await expect('Helfer sagt den Vortrag ab', api('PATCH', slotMask('talkCancelled'), { fields: { talkCancelled: { booleanValue: true } } }, helper.token), false);
await expect('Helfer ändert Markierung und Notiz zusammen', api('PATCH', slotMask('confirmed', 'note'), { fields: { confirmed: { booleanValue: true }, note: { stringValue: 'x' } } }, helper.token), false);
await expect('Helfer ersetzt den ganzen Termin', putSlot(HELPER_SLOT, { ...base, ...speaker, note: 'x' }, helper.token), false);
await expect('Helfer legt einen Termin an', putSlot('2027-01-10', base, helper.token), false);
await expect('Helfer löscht einen Termin', api('DELETE', `${SLOT}/${HELPER_SLOT}`, undefined, helper.token), false);
await expect('Helfer legt einen externen Redner an', post(EXT_SPEAKERS, validSpeaker, helper.token), false);
await expect('Helfer ändert ein Mitglied', api('PATCH', `${MEMBERS}/${plain.uid}${approvedMask}`, falseApproved, helper.token), false);
await expect('Helfer vergibt sich eine Rolle', api('PATCH', rolesMask(helper.uid), rolesBody('TALK_HELPER', 'CONGREGATION_ADMIN'), helper.token), false);

await expect('Nicht freigegebener Helfer liest keine Termine', api('GET', SLOT, undefined, pendingHelper.token), false);
await expect('Nicht freigegebener Helfer markiert nicht', api('PATCH', confirmedMask, confirmedBody(true), pendingHelper.token), false);
await expect('Helfer liest die Termine einer anderen Versammlung nicht', api('GET', `${OTHER_CONG}/talkSchedule`, undefined, helper.token), false);
await expect('Mitglied ohne Rolle markiert nicht', api('PATCH', confirmedMask, confirmedBody(true), plain.token), false);
await expect('Aufgabenplaner markiert nicht', api('PATCH', confirmedMask, confirmedBody(true), taskPlanner.token), false);

await expect('Planer markiert den Redner als informiert', api('PATCH', confirmedMask, confirmedBody(true), planner.token), true);
await expect('Planer ersetzt weiter den ganzen Termin', putSlot(HELPER_SLOT, { ...base, ...speaker, note: 'Planer' }), true);

await expect('Admin vergibt die Helfer-Rolle', api('PATCH', rolesMask(roleTarget.uid), rolesBody('TALK_HELPER'), congAdmin.token), true);
await expect('Helfer-Rolle zusammen mit der Planerrolle', api('PATCH', rolesMask(roleTarget.uid), rolesBody('TALK_HELPER', 'TALK_PLANNER'), congAdmin.token), true);
await expect('Selbstregistrierung mit Helfer-Rolle', api('PATCH', registerPath(helperNewcomer), registerBody('helpernewcomer@test.de', { roles: ['TALK_HELPER'] }), helperNewcomer.token), false);

console.log(`${count} checks, ${failures.length} failures`);
failures.forEach((failure) => console.log(`FAILURE: ${failure}`));
process.exitCode = failures.length === 0 ? 0 : 1;
