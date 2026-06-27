"use client";

import React, { Suspense } from "react";
import { useRouter } from "next/navigation";
import { XCircle, ArrowRight, ShieldAlert } from "lucide-react";
import { useSelector } from "react-redux";
import { RootState } from "../../../store/store";
import { translations } from "@/lib/translations";

function FailureCallbackContent() {
  const router = useRouter();
  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 text-foreground">
      <div className="w-full max-w-md bg-card border border-border rounded-2xl p-8 shadow-xl text-center space-y-6">
        
        <div className="w-16 h-16 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center mx-auto text-red-500">
          <XCircle className="w-8 h-8" />
        </div>
        
        <div className="space-y-2">
          <h2 className="text-2xl font-bold text-foreground">Payment Failed</h2>
          <p className="text-sm text-muted-foreground">
            {t.payFailureMsg}
          </p>
        </div>

        <div className="bg-muted/40 border border-border rounded-xl p-4 text-left text-xs space-y-2.5 flex items-start gap-2.5">
          <ShieldAlert className="w-5 h-5 text-yellow-500 shrink-0 mt-0.5" />
          <div className="space-y-1 text-muted-foreground">
            <p className="font-semibold text-foreground">What happened?</p>
            <p>Your session was either cancelled, the wallet integration timed out, or insufficient balances were encountered.</p>
          </div>
        </div>

        <div className="flex flex-col sm:flex-row gap-3 pt-2">
          <button
            onClick={() => router.push("/lawyers")}
            className="flex-1 py-2.5 bg-muted hover:bg-muted/80 text-foreground text-xs font-semibold rounded-xl border border-border transition-all"
          >
            Back to Lawyers
          </button>
          <button
            onClick={() => router.push("/dashboard")}
            className="flex-1 py-2.5 bg-primary hover:bg-primary/90 text-primary-foreground text-xs font-semibold rounded-xl shadow-md shadow-primary/10 transition-all"
          >
            Go to Dashboard
          </button>
        </div>

      </div>
    </div>
  );
}

export default function FailureCallback() {
  return (
    <Suspense fallback={
      <div className="flex min-h-screen items-center justify-center bg-background text-foreground">
        <div className="w-8 h-8 rounded-full border-2 border-primary border-t-transparent animate-spin" />
      </div>
    }>
      <FailureCallbackContent />
    </Suspense>
  );
}
