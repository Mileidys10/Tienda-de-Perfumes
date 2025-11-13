    package com.backend.perfumes.dto;

    import jakarta.validation.constraints.Email;
    import jakarta.validation.constraints.NotBlank;

    public class LoginRequest {



        @NotBlank(message = "You need to write your Email")
        @Email(message = "The email need to be a real email")
        private String email;

        @NotBlank(message = "The password is obligatory")
        private String password;

        public String getEmail() {
            return email;
        }
        public void setEmail(String email) {
            this.email = email;

        }

        public String getPassword() {

            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }


    }
