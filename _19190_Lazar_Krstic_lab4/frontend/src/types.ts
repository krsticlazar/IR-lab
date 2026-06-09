export type FieldOption = {
  value: string;
  label: string;
};

export type IndexStatus = {
  exists: boolean;
  documentCount: number;
  indexName: string;
  documentsDirectory: string;
};

export type IndexSummary = {
  indexedDocuments: number;
  indexName: string;
  documentsDirectory: string;
  indexedAt: string;
};

export type SearchResultItem = {
  id: string;
  fileName: string;
  title: string;
  sizeBytes: number;
  sizeText: string;
  modifiedAtText: string;
  indexedAtText: string;
  preview: string;
  score: number | null;
  downloadUrl: string;
};

export type SearchResponse = {
  results: SearchResultItem[];
  total: number;
  page: number;
  pageSize: number;
  totalPages: number;
  field: string;
  query: string;
};
