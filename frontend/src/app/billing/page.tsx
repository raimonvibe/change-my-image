'use client';
import React from "react";
import { useSession } from "next-auth/react";
import { CreditCard } from "lucide-react";
import { API_URL } from "../../env";

export default function BillingPage() {
  const { data: session } = useSession();
  const token = (session as any)?.idToken;

  const buy = async () => {
    const params = new URLSearchParams({
      successUrl: window.location.origin + '/account',
      cancelUrl: window.location.href,
    });
    const res = await fetch(`${API_URL}/api/billing/checkout?${params.toString()}`, {
      method: 'POST',
      headers: token ? { Authorization: `Bearer ${token}` } : {},
    });
    const data = await res.json();
    const url = data.url as string;
    window.location.href = url;
  };

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-sky-800">Pricing</h1>
      <div className="rounded-lg border bg-white p-6">
        <div className="text-slate-700">20 free conversions per day.</div>
        <div className="text-slate-700 mb-4">$1.98/month for unlimited conversions (optional monthly renewal).</div>
        <button onClick={buy} className="inline-flex items-center gap-2 rounded-md bg-sky-600 px-4 py-2 text-white hover:bg-sky-700">
          <CreditCard size={16} /> Subscribe
        </button>
      </div>
    </div>
  );
}
