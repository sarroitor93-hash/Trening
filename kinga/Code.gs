// Trening Kinga — zapasowy backend (Apps Script). Używany tylko, jeśli logowanie Google w aplikacji nie działa na iPhonie.
// Wdrożenie: script.google.com → Nowy projekt → wklej ten plik → uruchom setup() raz (zgoda) →
// Wdróż → Nowe wdrożenie → Aplikacja internetowa → Wykonuj jako: Ja, Dostęp: Każdy → skopiuj adres /exec.
// Klucz pokaże się w logu po setup(). Adres + klucz wpisz w aplikacji: ⚙ → Połączenie przez skrypt.
const TAB = "Treningi";
const HEAD = ["Data","Trening","Wersja","Partia","Ćwiczenie","Ciężar (kg)","Seria 1","Seria 2","Seria 3","Seria 4","Seria 5","Powt./sek. razem","Ocena","Czas (min)","Notatka ćw.","Notatka treningu","ID","Ciężary serii (kg)"];
const ID_COL = 17;

function setup() {
  const p = PropertiesService.getScriptProperties();
  if (!p.getProperty("SHEET")) {
    const ss = SpreadsheetApp.create("Trening Kinga");
    const sh = ss.getSheets()[0]; sh.setName(TAB); sh.appendRow(HEAD); sh.setFrozenRows(1);
    p.setProperty("SHEET", ss.getId());
  }
  if (!p.getProperty("KEY")) p.setProperty("KEY", Utilities.getUuid().replace(/-/g, "").slice(0, 16));
  Logger.log("Klucz: " + p.getProperty("KEY") + "  Arkusz: https://docs.google.com/spreadsheets/d/" + p.getProperty("SHEET"));
}
function sheet_() { return SpreadsheetApp.openById(PropertiesService.getScriptProperties().getProperty("SHEET")).getSheetByName(TAB); }
function ok_(o) { return ContentService.createTextOutput(JSON.stringify(Object.assign({ ok: true }, o || {}))).setMimeType(ContentService.MimeType.JSON); }
function err_(m) { return ContentService.createTextOutput(JSON.stringify({ ok: false, error: m })).setMimeType(ContentService.MimeType.JSON); }
function keyOk_(k) { return k && k === PropertiesService.getScriptProperties().getProperty("KEY"); }
function ids_(sh) { const n = sh.getLastRow() - 1; return n > 0 ? sh.getRange(2, ID_COL, n, 1).getValues().map(r => String(r[0])) : []; }

function doGet(e) {
  if (!keyOk_(e.parameter.key)) return err_("key");
  const sh = sheet_(), n = sh.getLastRow() - 1;
  const rows = n > 0 ? sh.getRange(2, 1, n, HEAD.length).getValues().map(r => r.map(v => v instanceof Date ? Utilities.formatDate(v, "Europe/Warsaw", "yyyy-MM-dd HH:mm:ss") : v)) : [];
  return ok_({ rows: rows });
}
function doPost(e) {
  const b = JSON.parse(e.postData.contents);
  if (!keyOk_(b.key)) return err_("key");
  const lock = LockService.getScriptLock(); lock.waitLock(20000);
  try {
    const sh = sheet_();
    if (b.action === "save") {
      if (ids_(sh).indexOf(String(b.id)) < 0 && b.rows && b.rows.length)
        sh.getRange(sh.getLastRow() + 1, 1, b.rows.length, HEAD.length).setValues(b.rows.map(r => { r = r.slice(0, HEAD.length); while (r.length < HEAD.length) r.push(""); return r; }));
      return ok_();
    }
    if (b.action === "delete") {
      const ids = ids_(sh);
      for (let i = ids.length - 1; i >= 0; i--) if (ids[i] === String(b.id)) sh.deleteRow(i + 2);
      return ok_();
    }
    return err_("action");
  } finally { lock.releaseLock(); }
}
