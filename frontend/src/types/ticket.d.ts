export interface TicketDTO {
    ticketId: number;
    eventCode?: string;
    eventId?: any;  // EventDTO object
    userId: number | any;  // Can be number or UserDTO object
    role: string;
    guestNumber?: number;
    invitationConfirmation?: boolean | null;
    assistConfirmation?: boolean | null;
    notes?: string;
    createdAt?: string;
    updatedAt?: string;
}

export interface UpdateTicketDTO {
    role?: string;
    guestNumber?: number;
    invitationConfirmation?: boolean | null;
    assistConfirmation?: boolean | null;
    notes?: string;
}

export interface EventTicketDTO {
    event: EventDTO;
    tickets: TicketDTO[];
}
