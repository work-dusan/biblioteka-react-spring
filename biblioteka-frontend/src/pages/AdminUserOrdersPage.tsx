import React, { useState, useEffect, useMemo } from "react";
import { useParams, useNavigate } from "react-router-dom";
import Navbar from "../components/Navbar";
import api from "../api/apiService";

export default function AdminUserOrdersPage() {
  const { id: userId } = useParams();
  const navigate = useNavigate();

  const [orders, setOrders] = useState([]);
  const [books, setBooks] = useState([]); // samo potrebne knjige

  // učitaj porudžbine pa knjige po ID-jevima (CSV), bez [] u URL-u
  useEffect(() => {
    const load = async () => {
      // 1) sve porudžbine tog korisnika
      const res = await api.get("/orders", { params: { userId } });
      const arr = Array.isArray(res.data) ? res.data : (res.data?.data ?? []);
      setOrders(arr);

      // 2) iz porudžbina pokupi jedinstvene bookId-ove koji NISU null
      const ids = Array.from(new Set(arr.map(o => o.bookId).filter(Boolean)));
      if (ids.length === 0) { setBooks([]); return; }

      // 3) uzmi baš te knjige; BE podržava ?ids=a,b,c
      const br = await api.get("/books", { params: { ids: ids.join(",") } });
      const list = Array.isArray(br.data) ? br.data : (br.data?.data ?? []);
      // normalizuj _id → id radi lakšeg mapiranja
      setBooks(list.map(b => ({ ...b, id: b.id ?? b._id })));
    };
    void load();
  }, [userId]);

  const booksById = useMemo(() => {
    const m = new Map();
    for (const b of books) m.set(b.id, b);
    return m;
  }, [books]);

  const active = useMemo(() => orders.filter(o => !o.returnedAt), [orders]);
  const history = useMemo(() => orders.filter(o => o.returnedAt), [orders]);

  const renderOrder = (o) => {
    // 1) prioritet: snapshot iz BE (ako postoji)
    const snap = o.bookSnapshot || o.displayBook || null;

    // 2) ako nema snapshot, probaj iz liste knjiga po id-u
    const live = o.bookId ? booksById.get(o.bookId) : null;

    const title = snap?.title ?? live?.title ?? "Nepoznata knjiga";
    const author = snap?.author ?? live?.author ?? "";
    const year = (snap?.year ?? live?.year) ?? "";
    const image = snap?.image ?? live?.image ?? "";

    return (
      <div key={o.id || o._id} className="bg-white p-4 rounded-2xl shadow flex items-center gap-4">
        <img
          src={image || "https://via.placeholder.com/80x120?text=No+Cover"}
          alt={title}
          className="w-16 h-24 object-cover rounded"
        />
        <div className="flex-1">
          <p className="font-semibold">{title}</p>
          <p className="text-sm text-gray-600">{author}{year ? ` (${year})` : ""}</p>
          <p className="text-sm text-gray-500">
            Iznajmljena: {new Date(o.rentedAt).toLocaleDateString()}
          </p>
          {o.returnedAt && (
            <p className="text-sm text-gray-500">
              Vraćena: {new Date(o.returnedAt).toLocaleDateString()}
            </p>
          )}
        </div>
      </div>
    );
  };

  return (
    <div className="min-h-screen bg-gray-100 flex flex-col">
      <header className="bg-white shadow">
        <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <Navbar />
          <button
            onClick={() => navigate(-1)}
            className="px-4 py-2 rounded-xl bg-slate-700 text-white hover:bg-slate-800"
          >
            Nazad
          </button>
        </div>
      </header>

      <main className="max-w-5xl mx-auto w-full p-6 space-y-10">
        <section>
          <h2 className="text-2xl font-semibold mb-4">Aktivne porudžbine</h2>
          {active.length
            ? <div className="space-y-4">{active.map(renderOrder)}</div>
            : <p className="text-gray-600">Nema aktivnih porudžbina.</p>
          }
        </section>

        <section>
          <h3 className="text-xl font-semibold mb-4">Istorija porudžbina</h3>
          {history.length
            ? <div className="space-y-4">{history.map(renderOrder)}</div>
            : <p className="text-gray-600">Još nema vraćenih porudžbina.</p>
          }
        </section>
      </main>
    </div>
  );
}
