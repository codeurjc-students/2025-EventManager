export interface Pagination {
    page: number;
    pageSize: number;
    totalItems: number;
}

export interface ApiResponse<T> {
    data: T;
    message?: string;
    error?: string;
}

export interface ErrorResponse {
    message: string;
    errors?: Record<string, string>;
}
