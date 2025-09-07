// src/pages/ProfilePage.tsx
import { useEffect, useMemo, useState } from "react";
import api from "../api/apiService";
import useAuth from "../hooks/useAuth";
import type { Book } from "../types";

// helpers
function pickArray(payload: any): any[] {
  if (payload?.data?.data?.items && Array.isArray(payload.data.data.items)) return payload.data.data.items;
  if (Array.isArray(payload?.data?.data)) return payload.data.data;
  if (Array.isArray(payload?.data)) return payload.data;
  if (Array.isArray(payload)) return payload;
  return [];
}

function normalizeBook(raw: any): Book {
  const id = raw?._id ?? raw?.id ?? "";
  return {
    id: typeof id === "string" ? id : String(id),
    title: raw?.title ?? "",
    author: raw?.author ?? "",
    year: String(raw?.year ?? ""),
    image: raw?.image ?? null,
    description: raw?.description ?? null,
    rentedBy: raw?.rentedBy ?? null,
  } as Book;
}

type DisplayBook = {
  id?: string | null;
  title?: string | null;
  author?: string | null;
  year?: string | number | null;
  image?: string | null;
} | null;

type OrderDto = {
  _id?: string;
  id?: string;
  userId: string;
  bookId: string | null;
  rentedAt: string;
  returnedAt: string | null;
  status?: string;
  // backend može slati ova polja:
  displayBook?: DisplayBook;
  bookSnapshot?: DisplayBook;
};

export default function ProfilePage() {
  const { user, updateProfile, toggleFavorite } = useAuth();

  // Omiljene (sada dohvaćene baš po ID-jevima iz user.favorites)
  const [favBooks, setFavBooks] = useState<Book[]>([]);
  const [orders, setOrders] = useState<OrderDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState("");

  // inline profile editing
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState(user?.name || "");
  const [email, setEmail] = useState(user?.email || "");
  const [password, setPassword] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveErr, setSaveErr] = useState("");
  const [saveOk, setSaveOk] = useState("");

  const userId = (user as any)?.id ?? (user as any)?._id ?? "";

  async function loadFavoriteBooks() {
  const favs = Array.isArray(user?.favorites) ? user!.favorites.filter(Boolean) : [];
  if (favs.length === 0) { setFavBooks([]); return; }

  const params = new URLSearchParams();
  for (const id of favs) params.append("id", id);   // -> ?id=a&id=b&id=c

  const res = await api.get("/books", { params });
  setFavBooks(pickArray(res).map(normalizeBook));
}

  async function loadOrders() {
    if (!userId) {
      setOrders([]);
      return;
    }
    // Samo moje porudžbine
    const res = await api.get("/orders", { params: { userId } });
    const arr = pickArray(res) as OrderDto[];
    setOrders(arr);
  }

  // Init učitavanje
  useEffect(() => {
    let alive = true;
    (async () => {
      try {
        setLoading(true);
        setErr("");
        await Promise.all([loadOrders(), loadFavoriteBooks()]);
      } catch (e: any) {
        if (!alive) return;
        setErr(e?.response?.data?.error || e?.message || "Greška pri učitavanju profila.");
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => { alive = false; };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userId]);

  // Ako se promeni lista omiljenih (npr. toggle iz drugih delova app) – osveži
  useEffect(() => {
    (async () => {
      try {
        await loadFavoriteBooks();
      } catch {
        /* ignore */
      }
    })();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [JSON.stringify(user?.favorites || [])]);

  // mapa omiljenih knjiga (ako zatreba za prikaz)
  const favMap = useMemo(() => {
    const m = new Map<string, Book>();
    for (const b of favBooks) m.set(b.id, b);
    return m;
  }, [favBooks]);

  // helper: iz order-a izvući knjigu za prikaz (displayBook -> snapshot -> pokušaj lookupa u omiljenima)
  const resolveBook = (o: OrderDto): DisplayBook => {
    if (o.displayBook) return o.displayBook;
    if (o.bookSnapshot) return o.bookSnapshot;
    if (o.bookId && favMap.has(o.bookId)) {
      const b = favMap.get(o.bookId)!;
      return { id: b.id, title: b.title, author: b.author, year: b.year, image: b.image };
    }
    return null;
  };

  // stabilan key za React (izbegava warning)
  const orderKey = (o: OrderDto) => o._id || o.id || `${o.userId}-${o.bookId || "none"}-${o.rentedAt}`;

  const myActive = useMemo(
    () => orders.filter(o => !o.returnedAt && (o.status ?? "active") === "active"),
    [orders]
  );

  const history = useMemo(
    () => orders.slice().sort((a, b) => (b.rentedAt || "").localeCompare(a.rentedAt || "")),
    [orders]
  );

  // SADA su “favorites” već upravo favBooks (nema više client-side filtriranja nad svim knjigama)
  const favorites = favBooks;

  function validate() {
    if (!name.trim()) return "Ime ne može biti prazno.";
    const emailRe = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRe.test(email.trim())) return "Neispravan email format.";
    if (password && password.length < 6) return "Lozinka mora imati najmanje 6 karaktera.";
    return "";
  }

  async function handleUpdate(e: React.FormEvent) {
    e.preventDefault();
    setSaveErr("");
    setSaveOk("");

    const noChange = name.trim() === (user?.name || "") && email.trim() === (user?.email || "") && password.trim() === "";
    if (noChange) {
      setSaveErr("Nema izmena za čuvanje.");
      return;
    }

    const v = validate();
    if (v) { setSaveErr(v); return; }

    setSaving(true);
    const body: { name?: string; email?: string; password?: string } = { name: name.trim(), email: email.trim() };
    if (password.trim()) body.password = password.trim();

    const res = await updateProfile(body);
    if (res.success) {
      setSaveOk("Podaci su uspešno sačuvani.");
      setPassword("");
      setEditing(false);
    } else {
      setSaveErr(res.message || "Greška pri ažuriranju podataka.");
    }
    setSaving(false);
  }

  async function handleReturn(orderId: string) {
    try {
      await api.patch(`/orders/${orderId}/return`, {});
      await Promise.all([loadOrders(), loadFavoriteBooks()]);
    } catch (e) {
      console.error("Greška pri vraćanju knjige:", e);
    }
  }

  async function handleToggleFavorite(bookId: string) {
    try {
      await toggleFavorite(bookId);     // optimistički ažurira user.favorites
      await loadFavoriteBooks();        // zatim sigurno osveži listu knjiga
    } catch (e) {
      console.error("Greška pri (un)favorite:", e);
    }
  }

  if (loading) return <div className="p-4">Učitavanje…</div>;
  if (err) return <div className="p-4 text-red-600">{err}</div>;

  return (
    <div className="max-w-4xl mx-auto p-4">
      <h1 className="text-2xl font-bold mb-4">Moj profil</h1>

      {!editing ? (
        <div className="mb-6">
          <p><b>Ime:</b> {user?.name}</p>
          <p><b>Email:</b> {user?.email}</p>
          <button
            onClick={() => { setSaveErr(""); setSaveOk(""); setName(user?.name || ""); setEmail(user?.email || ""); setPassword(""); setEditing(true); }}
            className="mt-3 bg-blue-600 text-white px-3 py-1 rounded hover:bg-blue-700"
          >
            Izmeni podatke
          </button>
        </div>
      ) : (
        <form onSubmit={handleUpdate} className="mb-6 grid gap-3 max-w-md">
          {saveErr && <div className="text-red-600">{saveErr}</div>}
          {saveOk && <div className="text-green-600">{saveOk}</div>}

          <div>
            <label className="block text-sm mb-1">Ime i prezime</label>
            <input className="w-full border px-3 py-2 rounded" value={name} onChange={(e) => setName(e.target.value)} autoComplete="name" />
          </div>
          <div>
            <label className="block text-sm mb-1">Email</label>
            <input type="email" className="w-full border px-3 py-2 rounded" value={email} onChange={(e) => setEmail(e.target.value)} autoComplete="email" />
          </div>
          <div>
            <label className="block text-sm mb-1">Nova lozinka (opciono)</label>
            <input type="password" className="w-full border px-3 py-2 rounded" value={password} onChange={(e) => setPassword(e.target.value)} autoComplete="new-password" />
          </div>

          <div className="flex gap-2">
            <button type="submit" disabled={saving} className={`bg-green-600 text-white px-3 py-1 rounded ${saving ? "opacity-70 cursor-not-allowed" : "hover:bg-green-700"}`}>
              {saving ? "Čuvam..." : "Sačuvaj"}
            </button>
            <button type="button" onClick={() => { setEditing(false); setSaveErr(""); setSaveOk(""); }} className="bg-gray-300 px-3 py-1 rounded">Otkaži</button>
          </div>
        </form>
      )}

      {/* Aktivna zaduženja */}
      <h2 className="text-xl font-semibold mb-2">Aktivna zaduženja</h2>
      {myActive.length === 0 ? (
        <div className="text-gray-600 mb-6">Nema aktivnih zaduženja.</div>
      ) : (
        <ul className="space-y-2 mb-6">
          {myActive.map((o) => {
            const b = resolveBook(o);
            return (
              <li key={orderKey(o)} className="border p-3 rounded flex items-center justify-between">
                <div>
                  <p><b>{b?.title || "Obrisana knjiga"}</b> — {b?.author || ""} ({b?.year || ""})</p>
                  <p className="text-sm text-gray-500">Iznajmljeno: {new Date(o.rentedAt).toLocaleDateString()}</p>
                </div>
                <button onClick={() => handleReturn(o._id || o.id!)} className="text-sm px-3 py-1 rounded bg-slate-600 text-white hover:bg-slate-700">
                  Vrati knjigu
                </button>
              </li>
            );
          })}
        </ul>
      )}

      {/* Istorija */}
      <h2 className="text-xl font-semibold mb-2">Istorija</h2>
      {history.length === 0 ? (
        <div className="text-gray-600 mb-6">Nema istorije zaduženja.</div>
      ) : (
        <ul className="space-y-2 mb-6">
          {history.map((o) => {
            const b = resolveBook(o);
            return (
              <li key={orderKey(o)} className="border p-3 rounded">
                <p><b>{b?.title || "Obrisana knjiga"}</b> — {b?.author || ""} ({b?.year || ""})</p>
                <p className="text-sm text-gray-500">
                  Iznajmljeno: {new Date(o.rentedAt).toLocaleString()}
                  {o.returnedAt && <span> • Vraćeno: {new Date(o.returnedAt).toLocaleString()}</span>}
                </p>
              </li>
            );
          })}
        </ul>
      )}

      {/* Omiljene */}
      <h2 className="text-xl font-semibold mb-2">Omiljene</h2>
      {favorites.length === 0 ? (
        <div className="text-gray-600">Nema omiljenih.</div>
      ) : (
        <ul className="space-y-2">
          {favorites.map((b) => (
            <li key={b.id} className="border p-3 rounded flex items-center justify-between">
              <p><b>{b.title}</b> — {b.author} ({b.year})</p>
              <button onClick={() => handleToggleFavorite(b.id)} className="text-sm px-3 py-1 rounded bg-amber-500 text-white hover:bg-amber-600">
                Ukloni iz omiljenih
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
