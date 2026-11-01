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

// Mitgliedsdokument im neuen Format (ADR-0012): nur `roles`.
const memberDoc = (email, roles) => fields({ email, displayName: email, approved: true, roles });
// Dokument im alten Format (ohne `roles`, nur `role`/`permissions`): seit CP-44 hat es keine Rechte mehr.
const legacyDoc = (email, role, permissions, extra = {}) =>
  fields({ email, displayName: email, approved: true, role, permissions, ...extra });

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
await api('PATCH', `${CONG}/members/${congAdmin.uid}`, fields({ email: 'congadmin@test.de', displayName: 'ca', approved: true, roles: ['CONGREGATION_ADMIN'] }), owner);
const adminPlanner = await signUp('adminplanner@test.de');
await api('PATCH', `${CONG}/members/${adminPlanner.uid}`, fields({ email: 'adminplanner@test.de', displayName: 'ap', approved: true, roles: ['CONGREGATION_ADMIN', 'TALK_PLANNER'] }), owner);
// Ein eigener Admin für die Tests, in denen ein Admin die eigene Rollenliste ändert.
const selfAdmin = await signUp('selfadmin@test.de');
await api('PATCH', `${CONG}/members/${selfAdmin.uid}`, memberDoc('selfadmin@test.de', ['CONGREGATION_ADMIN']), owner);
// Dokumente im alten Format; die Rules kennen den Rückfall nicht mehr.
const legacyAdmin = await signUp('legacyadmin@test.de');
await api('PATCH', `${CONG}/members/${legacyAdmin.uid}`, legacyDoc('legacyadmin@test.de', 'CONGREGATION_ADMIN', [], { firstName: 'Leo', lastName: 'Legacy' }), owner);
const legacyPlanner = await signUp('legacyplanner@test.de');
await api('PATCH', `${CONG}/members/${legacyPlanner.uid}`, legacyDoc('legacyplanner@test.de', 'MEMBER', ['DUMMY', 'PLAN_TALKS']), owner);
const legacyViewer = await signUp('legacyviewer@test.de');
await api('PATCH', `${CONG}/members/${legacyViewer.uid}`, legacyDoc('legacyviewer@test.de', 'MEMBER', ['DUMMY', 'VIEW_MEMBERS']), owner);
const OTHER_CONG ='region/R/subregion/S/branch/B/circuit/C/congregation/OTHER';
await api('PATCH', CONG, fields({ name: 'Berlin', midweek_meeting_time: '19:00', midweek_meeting_weekday: 3 }), owner);
await api('PATCH', OTHER_CONG, fields({ name: 'Hamburg', midweek_meeting_time: '19:00', midweek_meeting_weekday: 3 }), owner);

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
  phone: '0170 1234',
  email: 'eva@example.org',
  talkNumbers: [12, 45],
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
await expect('externer Redner mit zu langer Telefonnummer', post(EXT_SPEAKERS, { ...validSpeaker, phone: '1'.repeat(31) }), false);
await expect('externer Redner mit Zusatzfeld', post(EXT_SPEAKERS, { ...validSpeaker, x: 'y' }), false);
await expect('externes Verzeichnis lesen als Planer', api('GET', EXT_SPEAKERS, undefined, planner.token), true);
await expect('externe Kreise lesen ohne Planerrolle', api('GET', EXT_CIRCUITS, undefined, plain.token), false);
await expect('externe Redner lesen ohne Planerrolle', api('GET', EXT_SPEAKERS, undefined, plain.token), false);
await expect('externen Redner anlegen ohne Planerrolle', post(EXT_SPEAKERS, validSpeaker, plain.token), false);
await expect('alte Liste externalSpeakers ist nicht mehr beschreibbar', post(`${CONG}/externalSpeakers`, validSpeaker), false);

// ---------- Vortragshistorie ----------
const HIST = `${CONG}/talkHistory`;
const putHistory = (number, data, token = planner.token) => api('PATCH', `${HIST}/${number}`, fields(data), token);
await expect('Vortragshistorie anlegen', putHistory('7', { heldOn: ['2026-03-01', '2026-10-25'] }), true);
await expect('Vortragshistorie mit geplanten Tagen', putHistory('6', { heldOn: ['2026-03-01'], plannedOn: ['2026-03-01'] }), true);
await expect('Vortragshistorie mit Rednern', putHistory('6', { heldOn: ['2026-03-01'], speakers: { '2026-03-01': 'Muster, Max' } }), true);
await expect('Vortragshistorie mit ungültigen Rednern', putHistory('6', { heldOn: ['2026-03-01'], speakers: 'x' }), false);
await expect('Vortragshistorie mit ungültigen geplanten Tagen',putHistory('6', { heldOn: ['2026-03-01'], plannedOn: 'x' }), false);
await expect('Vortragshistorie mit ungültiger Nummer', putHistory('abc', { heldOn: ['2026-03-01'] }), false);
await expect('Vortragshistorie ohne Tage', putHistory('8', { heldOn: [] }), false);
await expect('Vortragshistorie mit Zusatzfeld', putHistory('8', { heldOn: ['2026-03-01'], x: 'y' }), false);
await expect('Vortragshistorie lesen als Planer', api('GET', HIST, undefined, planner.token), true);
await expect('Vortragshistorie lesen ohne Planerrolle', api('GET', HIST, undefined, plain.token), false);
await expect('Vortragshistorie anlegen ohne Planerrolle', putHistory('9', { heldOn: ['2026-03-01'] }, plain.token), false);
await expect('Planer löscht Vortragshistorie', api('DELETE', `${HIST}/7`, undefined, planner.token), true);

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

// ---------- Planen nur als Vortragsplaner: die Rolle Versammlungsadmin allein genügt nicht ----------
await expect('Versammlungsadmin ohne Planerrolle schreibt Termin', putSlot('2026-11-08', base, congAdmin.token), false);
await expect('Versammlungsadmin ohne Planerrolle liest Termine', api('GET', SLOT, undefined, congAdmin.token), false);
await expect('Aufgabenplaner schreibt keinen Termin', putSlot('2026-11-08', base, taskPlanner.token), false);
await expect('Versammlungsadmin mit Vortragsplaner-Rolle schreibt Termin', putSlot('2026-11-08', base, adminPlanner.token), true);

// ---------- Selbstregistrierung: ohne Rollen und ohne die veralteten Felder ----------
const registerBody = (email, extra = {}) => fields({ email, displayName: 'n', approved: false, roles: [], ...extra });
const newcomer = await signUp('newcomer@test.de');
const newcomer2 = await signUp('newcomer2@test.de');
const newcomer3 = await signUp('newcomer3@test.de');
const newcomer4 = await signUp('newcomer4@test.de');
const newcomer5 = await signUp('newcomer5@test.de');
const registerPath = (user) => `${CONG}/members/${user.uid}`;
await expect('Selbstregistrierung mit roles []', api('PATCH', registerPath(newcomer), registerBody('newcomer@test.de'), newcomer.token), true);
await expect('Selbstregistrierung mit Rolle', api('PATCH', registerPath(newcomer2), registerBody('newcomer2@test.de', { roles: ['CONGREGATION_ADMIN'] }), newcomer2.token), false);
await expect('Selbstregistrierung freigegeben', api('PATCH', registerPath(newcomer3), registerBody('newcomer3@test.de', { approved: true }), newcomer3.token), false);
await expect('Selbstregistrierung mit dem alten Feld role', api('PATCH', registerPath(newcomer4), registerBody('newcomer4@test.de', { role: 'MEMBER' }), newcomer4.token), false);
await expect('Selbstregistrierung ohne roles', api('PATCH', registerPath(newcomer5), fields({ email: 'newcomer5@test.de', displayName: 'n', approved: false }), newcomer5.token), false);
await expect('Selbstregistrierung mit fremder E-Mail-Adresse', api('PATCH', registerPath(newcomer5), registerBody('other@test.de'), newcomer5.token), false);


// ---------- Admin-Kontakte: Namen der Admins, auch vor der Freigabe lesbar ----------
const pending = await signUp('pending@test.de');
await api('PATCH', `${CONG}/members/${pending.uid}`, fields({ email: 'pending@test.de', displayName: 'p', approved: false, roles: [] }), owner);
const outsider = await signUp('outsider@test.de');
const namedAdmin = await signUp('named@test.de');
await api('PATCH', `${CONG}/members/${namedAdmin.uid}`, fields({ email: 'named@test.de', displayName: 'n', approved: true, roles: ['CONGREGATION_ADMIN'], firstName: 'Anna', lastName: 'Admin' }), owner);
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
await expect('Global Admin ändert den Namen der Versammlung', api('PATCH', `${CONG}${mask('name')}`, { fields: { name: { stringValue: 'Berlin-Mitte' } } }, admin.token), true);
await expect('Global Admin ändert die Zeiten der Versammlung nicht', api('PATCH', `${CONG}${mask('midweek_meeting_time')}`, time('18:15'), admin.token), false);
await expect('Global Admin ändert ein fremdes Feld der Versammlung nicht', api('PATCH', `${CONG}${mask('secret')}`, { fields: { secret: { stringValue: 'x' } } }, admin.token), false);
const NEW_CONG = 'region/R/subregion/S/branch/B/circuit/C/congregation/NEW';
await expect('Global Admin legt eine Versammlung an', api('PATCH', NEW_CONG, fields({ name: 'Neu', midweek_meeting_time: '19:00', midweek_meeting_weekday: 3 }), admin.token), true);
await expect('Global Admin löscht eine Versammlung', api('DELETE', NEW_CONG, undefined, admin.token), true);
await expect('Global Admin pflegt weiter die Kreise', api('PATCH', 'region/R2', { fields: {} }, admin.token), true);
await expect('Normaler Nutzer pflegt keine Kreise', api('PATCH', 'region/R3', { fields: {} }, plain.token), false);

// Doppelrolle: ein globaler Admin, der zugleich Vortragsplaner einer Versammlung ist, plant über seine Rolle.
const dualAdmin = await signUp('dualadmin@test.de');
await api('PATCH', `admins/${dualAdmin.uid}`, { fields: {} }, owner);
await api('PATCH', `${MEMBERS}/${dualAdmin.uid}`, memberDoc('dualadmin@test.de', ['TALK_PLANNER']), owner);
await expect('Global Admin mit Planerrolle plant über die Rolle', putSlot('2026-12-13', base, dualAdmin.token), true);
await expect('Global Admin mit Planerrolle liest Mitglieder über die Rolle', api('GET', MEMBERS, undefined, dualAdmin.token), true);
await expect('Global Admin mit Planerrolle ändert trotzdem kein Mitglied', api('PATCH', `${MEMBERS}/${plain.uid}${approvedMask}`, falseApproved, dualAdmin.token), false);
await expect('Global Admin mit Planerrolle pflegt weiter die Vortragsliste', api('PATCH', 'talkCatalog/77', talk('Dual'), dualAdmin.token), true);

// ---------- Alte Felder role/permissions: kein Rückfall mehr (CP-44) ----------
await expect('Admin im alten Format ändert die Versammlung nicht', api('PATCH', `${CONG}${mask('midweek_meeting_time')}`, time('18:45'), legacyAdmin.token), false);
await expect('PLAN_TALKS im alten Format schreibt keinen Termin', putSlot('2026-11-15', base, legacyPlanner.token), false);
await expect('VIEW_MEMBERS im alten Format listet keine Mitglieder', api('GET', MEMBERS, undefined, legacyViewer.token), false);
await expect('Admin-Kontakt für einen Admin im alten Format', putContact(legacyAdmin.uid, contact('Leo', 'Legacy'), congAdmin.token), false);
await expect(
  'Admin schreibt ein Mitglied mit den alten Feldern',
  api('PATCH', `${MEMBERS}/${plain.uid}`, fields({ email: 'plain@test.de', displayName: 'plain@test.de', approved: true, roles: [], role: 'MEMBER', permissions: ['DUMMY'] }), congAdmin.token),
  false,
);

console.log(`${count} checks, ${failures.length} failures`);
failures.forEach((failure) => console.log(`FAILURE: ${failure}`));
process.exitCode = failures.length === 0 ? 0 : 1;
