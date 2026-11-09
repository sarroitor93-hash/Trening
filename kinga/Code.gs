// Trening Kinga — backend (Apps Script): zapis treningów do arkusza „Trening Kinga” na Dysku.
// Wdrożenie: script.google.com → Nowy projekt → wklej ten plik → uruchom setup() raz (zgoda) →
// Wdróż → Nowe wdrożenie → Aplikacja internetowa → Wykonuj jako: Ja, Dostęp: Każdy → skopiuj adres /exec.
// Aktualizacja kodu: wklej nową wersję → Wdróż → Zarządzaj wdrożeniami → ołówek → Wersja: Nowa wersja → Wdróż (adres /exec się nie zmienia).
// Klucz jest ten sam co w aplikacji (index.html, SCRIPT_KEY). Adres /exec wpisuje Claude w aplikacji.
const TAB = "Treningi";
const HEAD = ["Data","Trening","Wersja","Partia","Ćwiczenie","Ciężar (kg)","Seria 1","Seria 2","Seria 3","Seria 4","Seria 5","Powt./sek. razem","Ocena","Czas (min)","Notatka ćw.","Notatka treningu","ID","Ciężary serii (kg)"];
const ID_COL = 17;
const MTAB = "Pomiary";
const MHEAD = ["Data","Masa (kg)","Talia (cm)","Biodra (cm)","Udo (cm)","ID"];
const KEY = "uCLJkHkzxkxJQQIm";

function setup() {
  const p = PropertiesService.getScriptProperties();
  if (!p.getProperty("SHEET")) {
    const ss = SpreadsheetApp.create("Trening Kinga");
    ss.setSpreadsheetTimeZone("Europe/Warsaw");
    const sh = ss.getSheets()[0]; sh.setName(TAB); sh.appendRow(HEAD); sh.setFrozenRows(1);
    p.setProperty("SHEET", ss.getId());
  }
  SpreadsheetApp.openById(p.getProperty("SHEET")).setSpreadsheetTimeZone("Europe/Warsaw");
  Logger.log("Gotowe. Arkusz: https://docs.google.com/spreadsheets/d/" + p.getProperty("SHEET"));
}
function book_() { return SpreadsheetApp.openById(PropertiesService.getScriptProperties().getProperty("SHEET")); }
function sheet_() { return book_().getSheetByName(TAB); }
function msheet_() { const b = book_(); let sh = b.getSheetByName(MTAB); if (!sh) { sh = b.insertSheet(MTAB); sh.appendRow(MHEAD); sh.setFrozenRows(1); } return sh; }
function rows_(sh, w) { const n = sh.getLastRow() - 1; return n > 0 ? sh.getRange(2, 1, n, w).getValues().map(r => r.map(v => v instanceof Date ? Utilities.formatDate(v, "Europe/Warsaw", "yyyy-MM-dd HH:mm:ss") : v)) : []; }
function ok_(o) { return ContentService.createTextOutput(JSON.stringify(Object.assign({ ok: true }, o || {}))).setMimeType(ContentService.MimeType.JSON); }
function err_(m) { return ContentService.createTextOutput(JSON.stringify({ ok: false, error: m })).setMimeType(ContentService.MimeType.JSON); }
function keyOk_(k) { return k === KEY; }
function ids_(sh, col) { const n = sh.getLastRow() - 1; return n > 0 ? sh.getRange(2, col || ID_COL, n, 1).getValues().map(r => String(r[0])) : []; }

function doGet(e) {
  if (!keyOk_(e.parameter.key)) return err_("key");
  return ok_({ rows: rows_(sheet_(), HEAD.length), meas: rows_(msheet_(), MHEAD.length) });
}
function doPost(e) {
  const b = JSON.parse(e.postData.contents);
  if (!keyOk_(b.key)) return err_("key");
  const lock = LockService.getScriptLock(); lock.waitLock(20000);
  try {
    if (b.action === "save") {
      const m = b.tab === MTAB, sh = m ? msheet_() : sheet_(), H = m ? MHEAD : HEAD, col = m ? MHEAD.length : ID_COL;
      if (ids_(sh, col).indexOf(String(b.id)) < 0 && b.rows && b.rows.length)
        sh.getRange(sh.getLastRow() + 1, 1, b.rows.length, H.length).setValues(b.rows.map(r => { r = r.slice(0, H.length); while (r.length < H.length) r.push(""); return r; }));
      return ok_();
    }
    if (b.action === "delete") {
      for (const [sh, col] of [[sheet_(), ID_COL], [msheet_(), MHEAD.length]]) {
        const ids = ids_(sh, col);
        for (let i = ids.length - 1; i >= 0; i--) if (ids[i] === String(b.id)) sh.deleteRow(i + 2);
      }
      return ok_();
    }
    return err_("action");
  } finally { lock.releaseLock(); }
}
