import { ref } from 'vue';
import { getEvents, createEvent, updateEvent, getEventByCode } from '../api/index';

export const useEventStore = () => {
    const events = ref([]);
    const event = ref([]);
    const loading = ref(false);
    const error = ref(null);

    const fetchAllEvents = async (params: any) => {
        loading.value = true;
        error.value = null;
        
        try {
            const response = await getEvents(params);
            // getEvents ya devuelve response.data
            events.value = response;
            return response;
        } catch (err: any) {
            error.value = err.message || 'Failed to fetch events';
            throw err;
        } finally {
            loading.value = false;
        }
    };

    const addEvent = async (userId: number, eventData: any) => {
        loading.value = true;
        error.value = null;
        try {
            const response = await createEvent(userId, eventData);
            
            // La función createEvent ya devuelve response.data, así que response ES el evento
            event.value = response;
            // Si llega aquí, la operación fue exitosa
            error.value = null;
            // Devolver el evento creado para que la vista pueda acceder a él
            return response;
        } catch (err: any) {
            const errorMessage = err.response?.data?.message || err.response?.data?.error || err.message || 'Failed to create event';
            error.value = errorMessage;
            // Re-lanzar el error para que la vista pueda manejarlo también
            throw err;
        } finally {
            loading.value = false;
        }
    };

    const editEvent = async (eventCode:  string, eventData: any) => {
        loading.value = true;
        error.value = null;
        try {
            const response = await updateEvent(eventCode, eventData);
            // updateEvent ya devuelve response.data
            return response;
        } catch (err: any) {
            error.value = err.message || 'Failed to update event';
        } finally {
            loading.value = false;
        }
    };

    const fetchEventByCode = async (eventCode: string) => {
        loading.value = true;
        error.value = null;
        try {
            const response = await getEventByCode(eventCode);
            event.value = response;
            return response;
        } catch (err: any) {
            error.value = err.message || 'Failed to fetch event';
            throw err;
        } finally {
            loading.value = false;
        }
    };

    return {
        events,
        event,
        loading,
        error,
        fetchAllEvents,
        addEvent,
        editEvent,
        fetchEventByCode,
    };
};