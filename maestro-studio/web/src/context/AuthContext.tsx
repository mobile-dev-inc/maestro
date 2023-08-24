import React, { ReactNode, createContext, useContext } from "react";
import { API } from "../api/api";

interface AuthProviderProps {
  children: ReactNode;
}

interface AuthState {
  isAuthenticated: boolean;
  token: string | null | undefined;
}

const AuthContext = createContext<AuthState | undefined>(undefined);

export const AuthProvider: React.FC<AuthProviderProps> = ({ children }) => {
  const { data: token } = API.useAuth();

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated: !!token,
        token: token,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = (): AuthState => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error("useAuth must be used within an AuthProvider");
  }
  return context;
};
