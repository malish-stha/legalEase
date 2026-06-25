import { createSlice, PayloadAction } from "@reduxjs/toolkit";

interface AuthState {
  role: "USER" | "LAWYER" | "ADMIN" | null;
}

const initialState: AuthState = {
  role: null,
};

export const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setRole: (state, action: PayloadAction<"USER" | "LAWYER" | "ADMIN" | null>) => {
      state.role = action.payload;
    },
    clearAuth: (state) => {
      state.role = null;
    },
  },
});

export const { setRole, clearAuth } = authSlice.actions;
export default authSlice.reducer;
