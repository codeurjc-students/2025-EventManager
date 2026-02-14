import { ref } from 'vue';
import { enrollUserInEvent, getEventTickets, getEventInformation, updateTicket } from '../api/index';
import { TicketDTO } from '../types/ticket';

export const useTicketStore = () => {
  const tickets = ref<TicketDTO[]>([]);
  const ticketDetail = ref<TicketDTO | null>(null);
  const totalTickets = ref<number>(0);
  const currentPage = ref<number>(1);
  const pageSize = ref<number>(10);
  const loading = ref<boolean>(false);
  const error = ref<string | null>(null);

  const enrollUser = async (enrollmentData: any) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await enrollUserInEvent(enrollmentData);
      return response;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error enrolling user in event';
    } finally {
      loading.value = false;
    }
  };

  const fetchTicketDetail = async (eventCode: string, ticketId: number, userId: number) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await getEventInformation(eventCode, ticketId, userId);
      ticketDetail.value = response.ticket;
      return response;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error fetching ticket detail';
    } finally {
      loading.value = false;
    }
  };

  const editTicket = async (eventCode: string, ticketId: number, ticketUpdateData: any) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await updateTicket(eventCode, ticketId, ticketUpdateData);
      ticketDetail.value = response.ticket;
      return response;
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error updating ticket';
    } finally {
      loading.value = false;
    }
  };

  const fetchTickets = async (eventCode: string, params: any = {}) => {
    loading.value = true;
    error.value = null;
    try {
      const response = await getEventTickets(eventCode, { page: currentPage.value, pageSize: pageSize.value, ...params });
      // getEventTickets ya devuelve response.data, así que response ES el objeto con data y page
      tickets.value = response.data;
      totalTickets.value = response.page?.totalElements || 0;
      return {data: response.data, total: response.page?.totalElements || 0};
    } catch (err: any) {
      error.value = err.response?.data?.message || 'Error fetching tickets';
      return {data: [], total: 0};
    } finally {
      loading.value = false;
    }
  };

  const setPage = (page: number) => {
    currentPage.value = page;
  };

  return {
    tickets,
    ticketDetail,
    totalTickets,
    currentPage,
    pageSize,
    loading,
    error,
    enrollUser,
    fetchTickets,
    fetchTicketDetail,
    editTicket,
    setPage,
  };
};