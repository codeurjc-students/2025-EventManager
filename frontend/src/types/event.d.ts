export interface EventDTO {
    eventId: number;
    eventCode: string;
    name: string;
    description: string;
    date: string; // ISO date string
    location: string;
    createdBy: number;
    participants: number;
}

export interface EventWithTicketDTO {
    eventId: number;
    eventCode: string;
    name: string;
    description: string;
    date: string; // ISO date string
    location: string;
    createdBy: number;
    participants: number;
    ticketId: number;
}

export interface CreateUpdateEventDTO {
    eventCode: string;
    name: string;
    description: string;
    date: string; // ISO date string
    location: string;
}

export interface EventTicketDTO {
    ticketId: number;
    eventId: number;
    userId: number;
    ticketType: string;
    purchaseDate: string; // ISO date string
}

export interface PaginationDTO<T> {
    data: T[];
    total: number; // total number of items
    page: number; // current page number
    pageSize: number; // number of items per page
}
