"use client";

import React from "react";
import { useDispatch, useSelector } from "react-redux";
import { RootState } from "../../store/store";
import { setLanguage } from "../../store/slices/uiSlice";

export default function LanguageToggler() {
  const dispatch = useDispatch();
  const language = useSelector((state: RootState) => state.ui.language);

  return (
    <div className="flex items-center gap-1 bg-muted/50 p-1 rounded-md border border-border">
      <button
        onClick={() => dispatch(setLanguage("ne"))}
        className={`px-2.5 py-1 text-xs font-semibold transition-all cursor-pointer ${
          language === "ne"
            ? "bg-primary text-primary-foreground shadow-sm"
            : "text-foreground/60 hover:text-foreground hover:bg-muted/80"
        }`}
      >
        नेपाली
      </button>
      <button
        onClick={() => dispatch(setLanguage("en"))}
        className={`px-2.5 py-1 text-xs font-semibold transition-all cursor-pointer ${
          language === "en"
            ? "bg-primary text-primary-foreground shadow-sm"
            : "text-foreground/60 hover:text-foreground hover:bg-muted/80"
        }`}
      >
        EN
      </button>
    </div>
  );
}
