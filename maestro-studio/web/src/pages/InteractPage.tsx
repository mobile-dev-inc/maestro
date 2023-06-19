import { useEffect, useState } from "react";

import InteractableDevice from "../components/device-and-device-elements/InteractableDevice";
import ReplView from "../components/commands/ReplView";
import { DeviceScreen } from "../models";
import { API, wait } from "../api/api";
import { ActionModal } from "../ActionModal";
import { Input } from "../components/design-system/input";

const InteractPage = () => {
  const [deviceScreen, setDeviceScreen] = useState<DeviceScreen>();
  const [input, setInput] = useState("");
  const [footerHint, setFooterHint] = useState<string | null>();
  const [inspectedElementId, setInspectedElementId] =
    useState<string | null>(null);
  const inspectedElement =
    deviceScreen?.elements?.find((e) => e.id === inspectedElementId) || null;

  useEffect(() => {
    let running = true;
    (async () => {
      while (running) {
        try {
          const deviceScreen = await API.getDeviceScreen();
          setDeviceScreen(deviceScreen);
        } catch (e) {
          console.error(e);
          await wait(1000);
        }
      }
    })();
    return () => {
      running = false;
    };
  }, [setDeviceScreen]);

  if (!deviceScreen) return null;

  return (
    <div className="flex h-full overflow-hidden">
      <div className="px-8 py-6 bg-white dark:bg-slate-900 basis-1/2 lg:basis-5/12 relative gap-4 flex flex-col">
        <InteractableDevice
          deviceScreen={deviceScreen}
          onHint={setFooterHint}
          inspectedElement={inspectedElement}
          onInspectElement={(e) => setInspectedElementId(e?.id || null)}
        />
        <p className="text-xs text-center">
          Hold CMD (⌘) down to freely tap and swipe on the device screen
        </p>
        {footerHint && (
          <div className="absolute bottom-0 left-0 right-0 text-xs text-center bg-slate-100 dark:bg-slate-800 dark:text-white h-auto text-slate-800 overflow-hidden">
            {footerHint}
          </div>
        )}
      </div>
      <div className="flex flex-col flex-1 h-full overflow-hidden border-l border-slate-200 dark:border-slate-800 relative dark:bg-slate-900 dark:text-white">
        <div className="px-12 pt-6 pb-40 h-full overflow-hidden">
          <ReplView input={input} onInput={setInput} />
        </div>

        {inspectedElement && (
          <ActionModal
            deviceScreen={deviceScreen}
            uiElement={inspectedElement}
            onEdit={(example) => {
              if (example.status === "unavailable") return;
              setInput(example.content.trim());
              setInspectedElementId(null);
            }}
            onRun={(example) => {
              if (example.status === "unavailable") return;
              API.repl.runCommand(example.content);
              setInspectedElementId(null);
            }}
            onClose={() => setInspectedElementId(null)}
          />
        )}
      </div>
    </div>
  );
};

export default InteractPage;
