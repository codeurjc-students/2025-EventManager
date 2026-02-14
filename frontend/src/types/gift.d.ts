export interface GiftDTO {
  giftId: number;
  eventId: number;
  name: string;
  price: number;
  collected: number;
  creationUser: string;
  createdByHost: boolean;
  paidInFull: boolean;
  details?: string;
  url?: string;
  image?: string;
  userContributionList?: {
    userId: number;
    username: string;
    email: string;
    phoneNumber: string;
    amount: number;
  }[];
  participants?: {
    username: string;
    email: string;
    phone: string;
  }[];
}

export interface GiftCreateDTO {
    name: string;
    price: number;
    details?: string;
    url?: string;
    image?: any;
    creationUser: string;
}

export interface GiftUpdateDTO {
    giftId: number;
    name?: string;
    description?: string;
}
