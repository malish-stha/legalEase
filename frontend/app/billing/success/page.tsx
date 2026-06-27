"use client";

import React, { useState, useEffect, Suspense } from "react";
import { useSearchParams, useRouter } from "next/navigation";
import { useAuth } from "@clerk/nextjs";
import axios from "axios";
import { CheckCircle2, XCircle, Loader2, ArrowRight, ShieldAlert } from "lucide-react";
import { useSelector } from "react-redux";
import { RootState } from "../../../store/store";
import { translations } from "@/lib/translations";

function SuccessCallbackContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const { getToken } = useAuth();
  const language = useSelector((state: RootState) => state.ui.language);
  const t = translations[language];

  const [loading, setLoading] = useState(true);
  const [success, setSuccess] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [txDetails, setTxDetails] = useState<any>(null);

  useEffect(() => {
    const verifyPayment = async () => {
      try {
        const token = await getToken();
        if (!token) {
          setErrorMsg("Authentication required.");
          setLoading(false);
          return;
        }

        // 1. Check eSewa response (returns ?data=...)
        const esewaData = searchParams.get("data");
        
        // 2. Check Khalti response (returns ?pidx=...)
        const khaltiPidx = searchParams.get("pidx");

        if (esewaData) {
          // Verify eSewa
          const response = await axios.post(
            `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/billing/esewa/verify`,
            { data: esewaData },
            { headers: { Authorization: `Bearer ${token}` } }
          );
          setTxDetails(response.data);
          setSuccess(true);
        } else if (khaltiPidx) {
          // Verify Khalti
          const response = await axios.post(
            `${process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080"}/api/billing/khalti/verify`,
            { pidx: khaltiPidx },
            { headers: { Authorization: `Bearer ${token}` } }
          );
          setTxDetails(response.data);
          setSuccess(true);
        } else {
          setErrorMsg("Invalid payment callback parameters.");
        }
      } catch (err: any) {
        console.error("Payment verification failed", err);
        setErrorMsg(err.response?.data?.message || t.payFailureMsg);
      } finally {
        setLoading(false);
      }
    };

    verifyPayment();
  }, [searchParams, getToken, t.payFailureMsg]);

  return (
    <div className="min-h-screen bg-background flex flex-col items-center justify-center p-6 text-foreground">
      <div className="w-full max-w-md bg-card border border-border rounded-2xl p-8 shadow-xl text-center space-y-6">
        
        {loading && (
          <div className="space-y-4 py-8">
            <Loader2 className="w-12 h-12 text-primary animate-spin mx-auto" />
            <h2 className="text-xl font-bold text-foreground">Verifying Payment...</h2>
            <p className="text-sm text-muted-foreground">Communicating with the payment provider to secure your booking.</p>
          </div>
        )}

        {!loading && success && (
          <div className="space-y-6">
            <div className="w-16 h-16 rounded-full bg-green-500/10 border border-green-500/20 flex items-center justify-center mx-auto text-green-500">
              <CheckCircle2 className="w-8 h-8" />
            </div>
            <div className="space-y-2">
              <h2 className="text-2xl font-bold text-foreground">{t.paySuccessMsg}</h2>
              <p className="text-sm text-muted-foreground">Your transaction has been verified successfully.</p>
            </div>

            {txDetails && (
              <div className="bg-muted/40 border border-border rounded-xl p-4 text-left text-xs space-y-2.5">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Amount:</span>
                  <span className="font-bold text-foreground">Rs. {txDetails.amount.toLocaleString()}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Gateway:</span>
                  <span className="font-bold text-foreground uppercase">{txDetails.gateway}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Reference/Tx ID:</span>
                  <span className="font-mono text-muted-foreground/80 truncate max-w-[200px]">{txDetails.transactionId}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Status:</span>
                  <span className="font-bold text-green-500">SUCCESS</span>
                </div>
              </div>
            )}

            <button
              onClick={() => router.push("/dashboard")}
              className="w-full py-3 bg-primary hover:bg-primary/90 text-primary-foreground font-bold rounded-xl shadow-lg shadow-primary/20 hover:shadow-primary/40 transition-all flex items-center justify-center gap-2 text-sm"
            >
              Go to Dashboard
              <ArrowRight className="w-4 h-4" />
            </button>
          </div>
        )}

        {!loading && !success && (
          <div className="space-y-6">
            <div className="w-16 h-16 rounded-full bg-red-500/10 border border-red-500/20 flex items-center justify-center mx-auto text-red-500">
              <XCircle className="w-8 h-8" />
            </div>
            <div className="space-y-2">
              <h2 className="text-2xl font-bold text-foreground">Verification Failed</h2>
              <p className="text-sm text-red-500 bg-red-500/10 border border-red-500/20 rounded-xl p-3 font-semibold mt-2">{errorMsg}</p>
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
        )}

      </div>
    </div>
  );
}

export default function SuccessCallback() {
  return (
    <Suspense fallback={
      <div className="flex min-h-screen items-center justify-center bg-background text-foreground">
        <Loader2 className="w-8 h-8 text-primary animate-spin" />
      </div>
    }>
      <SuccessCallbackContent />
    </Suspense>
  );
}
