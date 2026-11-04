// Startet den Rules-Test: kopiert ../firestore.rules in diesen Ordner (die Firebase CLI erlaubt keine Rules außerhalb
// des Projektordners; die Kopie ist per .gitignore ausgeschlossen) und führt rules_test.mjs im Emulator aus.
import { spawnSync } from 'node:child_process';
import { copyFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
copyFileSync(join(here, '..', 'firestore.rules'), join(here, 'firestore.rules'));

const result = spawnSync(
  'firebase',
  ['emulators:exec', '--only', 'auth,firestore', '--project', 'demo-rules-test', '"node rules_test.mjs"'],
  { cwd: here, stdio: 'inherit', shell: true },
);
process.exitCode = result.status ?? 1;
