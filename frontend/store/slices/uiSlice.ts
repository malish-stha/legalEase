import { createSlice, PayloadAction } from "@reduxjs/toolkit";

interface UiState {
  language: "ne" | "en";
}

const initialState: UiState = {
  language: "ne", // default language is Nepali
};

const uiSlice = createSlice({
  name: "ui",
  initialState,
  reducers: {
    setLanguage: (state, action: PayloadAction<"ne" | "en">) => {
      state.language = action.payload;
    },
  },
});

export const { setLanguage } = uiSlice.actions;
export default uiSlice.reducer;
