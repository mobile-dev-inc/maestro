import {
  BannerMessage,
  DeviceScreen,
  FormattedFlow,
  MockEvent,
  Repl,
  ReplCommand,
} from "../helpers/models";
import useSWR, { mutate, SWRConfiguration, SWRResponse } from "swr";
import useSWRSubscription, { SWRSubscriptionResponse } from "swr/subscription";

export type ReplResponse = {
  repl?: Repl;
  error?: any;
};

type GetMockDataResponse = {
  projectId: string;
  events: MockEvent[];
};

export class HttpError extends Error {
  constructor(public status: number, public message: string) {
    super(message);
  }
}

export const wait = async (durationMs: number) => {
  return new Promise((resolve) => setTimeout(resolve, durationMs));
};
const makeRequest = async <T>(
  method: string,
  path: string,
  body?: Object | undefined,
  type?: "json" | "text"
): Promise<T> => {
  const options: RequestInit = {
    method,
    headers: {
      Accept: "application/json",
      "Content-Type": "application/json",
    },
  };
  // Ensure body isn't set for GET or HEAD requests
  if (body && method !== "GET" && method !== "HEAD") {
    options.body = JSON.stringify(body);
  }
  const response = await fetch(path, options);
  if (!response.ok) {
    const responseBody = await response.text();
    throw new HttpError(response.status, responseBody);
  }
  if (type === "text") {
    return (await response.text()) as any as T;
  }
  return (await response.json()) as T;
};

const useSse = <T>(url: string): SWRSubscriptionResponse<T> => {
  return useSWRSubscription<T, any, string>(url, (key, { next }) => {
    const eventSource = new EventSource(key);
    eventSource.onmessage = (e) => {
      const repl: T = JSON.parse(e.data);
      next(null, repl);
    };
    eventSource.onerror = (error) => {
      next(error);
    };
    return () => eventSource.close();
  });
};

const useRepl = (): ReplResponse => {
  const { data: repl, error } = useSse<Repl>("/api/repl/sse");
  return { repl, error };
};

const useDeviceScreen = (): { deviceScreen?: DeviceScreen; error?: any } => {
  const { data: deviceScreen, error } = useSse<DeviceScreen>(
    "/api/device-screen/sse"
  );
  return { deviceScreen, error };
};

const fetcher = (url: string) => fetch(url).then((r) => r.json());

export const API = {
  useAuth: (
    config?: SWRConfiguration<string | null>
  ): SWRResponse<string | null> => {
    return useSWR(
      "/api/auth-token",
      () => makeRequest("GET", "/api/auth-token", undefined, "text"),
      config
    );
  },
  useDeviceScreen,
  useBannerMessage: (
    config?: SWRConfiguration<BannerMessage>
  ): SWRResponse<BannerMessage> => {
    return useSWR(
      "/api/banner-message",
      (url) => makeRequest("GET", url),
      config
    );
  },
  repl: {
    useRepl,
    runCommand: async (yaml: string): Promise<Repl> => {
      return makeRequest("POST", "/api/repl/command", { yaml });
    },
    runCommandsById: async (ids: string[]): Promise<Repl> => {
      return makeRequest("POST", "/api/repl/command", { ids });
    },
    deleteCommands: async (ids: string[]): Promise<Repl> => {
      return makeRequest("DELETE", "/api/repl/command", { ids });
    },
    reorderCommands: async (ids: string[]) => {
      makeRequest("POST", "/api/repl/command/reorder", { ids });
      mutate<Repl>(
        "/api/repl/sse",
        (repl) => {
          if (!repl) return undefined;
          const newCommands: ReplCommand[] = [];
          const idsAsSet = new Set(ids);
          repl.commands
            .filter((c) => !!c && idsAsSet.has(c.id))
            .forEach((c) => newCommands.push(c));
          return { ...repl, commands: newCommands };
        },
        {
          revalidate: false,
        }
      );
    },
    formatFlow: async (ids: string[]): Promise<FormattedFlow> => {
      return makeRequest("POST", "/api/repl/command/format", { ids });
    },
  },
  useMockData: (config?: SWRConfiguration) => {
    return useSWR<GetMockDataResponse>(
      "/api/mock-server/data",
      fetcher,
      config
    );
  },
};
