import { createSlice, PayloadAction } from "@reduxjs/toolkit";

export interface DocAnalysis {
  id: string;
  summary: string;
  riskLevel: "LOW" | "MEDIUM" | "HIGH";
  keyClauses: string; // JSON Array string
}

export interface Document {
  id: string;
  fileName: string;
  fileUrl: string;
  status: "PENDING" | "PROCESSING" | "DONE" | "FAILED";
  language: string;
  createdAt: string;
  analysis?: DocAnalysis;
}

interface DocumentState {
  list: Document[];
  currentDoc: Document | null;
  loading: boolean;
  error: string | null;
}

const initialState: DocumentState = {
  list: [],
  currentDoc: null,
  loading: false,
  error: null,
};

export const documentSlice = createSlice({
  name: "document",
  initialState,
  reducers: {
    setDocuments: (state, action: PayloadAction<Document[]>) => {
      state.list = action.payload;
    },
    setCurrentDoc: (state, action: PayloadAction<Document | null>) => {
      state.currentDoc = action.payload;
    },
    addDocument: (state, action: PayloadAction<Document>) => {
      state.list.unshift(action.payload);
    },
    updateDocumentStatus: (state, action: PayloadAction<{ id: string; status: Document["status"]; analysis?: DocAnalysis }>) => {
      const doc = state.list.find((d) => d.id === action.payload.id);
      if (doc) {
        doc.status = action.payload.status;
        if (action.payload.analysis) {
          doc.analysis = action.payload.analysis;
        }
      }
      if (state.currentDoc && state.currentDoc.id === action.payload.id) {
        state.currentDoc.status = action.payload.status;
        if (action.payload.analysis) {
          state.currentDoc.analysis = action.payload.analysis;
        }
      }
    },
    setLoading: (state, action: PayloadAction<boolean>) => {
      state.loading = action.payload;
    },
    setError: (state, action: PayloadAction<string | null>) => {
      state.error = action.payload;
    },
  },
});

export const { setDocuments, setCurrentDoc, addDocument, updateDocumentStatus, setLoading, setError } = documentSlice.actions;
export default documentSlice.reducer;
