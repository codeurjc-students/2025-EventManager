export interface UserDTO {
    userId: number;
    email: string;
    username: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
}

export interface UserCreateDTO {
    email: string;
    username: string;
    password: string;
    firstName: string;
    lastName: string;
    phoneNumber: string;
}

export interface UserUpdateDTO {
    firstName?: string;
    lastName?: string;
    phoneNumber?: string;
}

export interface UserPasswordDTO {
    password: string;
}

export interface UserForgottenPassword {
    email: string;
}
