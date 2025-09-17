'use client';

import React, { useCallback, useState } from 'react';
import { useSession } from 'next-auth/react';
import { CreditCard } from 'lucide-react';
import { API_URL } from '../../env';

export default function BillingPage() {
  const { data: session } = useSession();
  const token = (session as any)?.idToken as string | undefined;

  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const buy = useCallback(async () => {
    setErr(null);
    setLoading(true);

    try {
      const params = new URLSearchParams({
        successUrl: `${window.location.origin}/account`,
        cancelUrl: window.location.href,
      });

      const res = await fetch(`${API_URL}/api/billing/checkout?${params.toString()}`, {
        method: 'POST',
        // Als je cookies/sessies gebruikt aan backend-zijde, laat dan 'include' staan.
        credentials: 'include',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
      });

      if (!res.ok) {
        // Backend kan lege body teruggeven bij 4xx → lees als tekst om debug te helpen
        const text = await res.text().catch(() => '');
        throw new Error(`Checkout failed: ${res.status} ${res.statusText}${text ? ` — ${text}` : ''}`);
      }

      // Happy path: JSON parsen
      let data: unknown;
      try {
        data = await res.json();
      } catch {
        throw new Error('Invalid JSON response from server.');
      }

      const url = (data as { url?: unknown })?.url;
      if (typeof url !== 'string' || !url) {
        throw new Error('Missing checkout URL in response.');
      }

      // Naar Stripe (of jouw checkout)
      window.location.assign(url);
    } catch (e: unknown) {
      const message = e instanceof Error ? e.message : 'Unknown error';
      setErr(message);
      console.error('[billing] buy error:', e);
    } finally {
      setLoading(false);
    }
  }, [token]);

  return (
    <div className="space-y-4">
      <h1 className="text-xl font-semibold text-sky-800">Pricing</h1>

      <div className="rounded-lg border bg-white p-6">
        <div className="text-slate-700">20 free conversions per day.</div>
        <div className="text-slate-700 mb-4">$1.98/month for unlimited conversions (optional monthly renewal).</div>
        <button
          onClick={buy}
          disabled={loading}
          className="inline-flex items-center gap-2 rounded-md bg-sky-600 px-4 py-2 text-white hover:bg-sky-700 disabled:opacity-60 disabled:cursor-not-allowed"
        >
          <CreditCard size={16} />
          {loading ? 'Processing…' : 'Subscribe'}
        </button>

        <div className="text-slate-700 mt-4">5 free conversions per day.</div>
        <div className="text-slate-700 mb-4">$1 = 5 additional conversions.</div>
        <button
          onClick={buy}
          disabled={loading}
          className="inline-flex items-center gap-2 rounded-md bg-sky-600 px-4 py-2 text-white hover:bg-sky-700 disabled:opacity-60 disabled:cursor-not-allowed"
        >
          <CreditCard size={16} />
          {loading ? 'Processing…' : 'Buy pack'}
        </button>

        {err && (
          <p className="mt-3 text-sm text-red-600 break-words">
            {err}
          </p>
        )}
      </div>
    </div>
  );
}
