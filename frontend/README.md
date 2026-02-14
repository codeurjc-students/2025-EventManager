# README for Event Management Frontend

## Project Overview
This project is a frontend application for managing events, built using Vue.js and Vite. It interacts with a Spring Boot backend to provide functionalities for user authentication, event management, ticket handling, and gift management.

## Project Structure
- **src/**: Contains the source code for the application.
  - **main.ts**: Entry point of the Vue application.
  - **App.vue**: Root component of the application.
  - **api/**: Functions to interact with the backend API.
  - **assets/**: Static assets like images and styles.
  - **components/**: Reusable Vue components.
  - **layouts/**: Layout components for the application.
  - **router/**: Routing configuration for the application.
  - **stores/**: Vuex stores for managing application state.
  - **views/**: Different views for the application.
  - **types/**: TypeScript type definitions.

## Setup Instructions
1. **Clone the repository**:
   ```bash
   git clone <repository-url>
   cd frontend
   ```

2. **Install dependencies**:
   ```bash
   npm install
   ```

3. **Run the application**:
   ```bash
   npm run dev
   ```

4. **Build the application for production**:
   ```bash
   npm run build
   ```

## Usage
- Navigate to the application in your web browser at `http://localhost:3000` (or the port specified in your Vite configuration).
- Use the login view to authenticate users.
- Access various functionalities such as creating events, joining events, and managing tickets and gifts.

## Contributing
Contributions are welcome! Please open an issue or submit a pull request for any improvements or bug fixes.

## License
This project is licensed under the MIT License. See the LICENSE file for details.