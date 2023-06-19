import { memo, useEffect, useRef, useState } from "react";
import { API } from "../../api/api";
import { FormattedFlow, ReplCommand } from "../../models";
import { Reorder } from "framer-motion";
import { SaveFlowModal } from "../../SaveFlowModal";
import { useConfirmationDialog } from "../../ConfirmationDialog";
import { Button } from "../design-system/button";
import ReplHeader from "./ReplHeader";
import CommandRow from "./CommandRow";
import CommandInput from "./CommandInput";

const getFlowText = (selected: ReplCommand[]): string => {
  return selected
    .map((c) => (c.yaml.endsWith("\n") ? c.yaml : `${c.yaml}\n`))
    .join("");
};

const ReplView = ({
  input,
  onInput,
}: {
  input: string;
  onInput: (input: string) => void;
}) => {
  const listRef = useRef<HTMLElement>();
  const [replError, setReplError] = useState<string | null>(null);
  const [_selected, setSelected] = useState<string[]>([]);
  const [dragging, setDragging] = useState(false);
  const [formattedFlow, setFormattedFlow] =
    useState<FormattedFlow | null>(null);
  const { error, repl } = API.repl.useRepl();
  const listSize = repl?.commands.length || 0;
  const previousListSize = useRef(0);

  const [showConfirmationDialog, Dialog] = useConfirmationDialog(() =>
    API.repl.deleteCommands(selectedIds)
  );

  // Scroll to bottom when new commands are added
  useEffect(() => {
    const listSizeChange = listSize - previousListSize.current;
    if (listSizeChange > 0 && listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight;
    }
    previousListSize.current = listSize;
  }, [listSize]);

  if (error) {
    return <div>Error fetching repl</div>;
  }

  if (!repl) {
    return null;
  }

  const selectedCommands = _selected
    .map((id) => repl.commands.find((c) => c.id === id))
    .filter((c): c is ReplCommand => !!c);
  const selectedIds = selectedCommands.map((c) => c.id);

  const runCommand = async () => {
    if (!input) return;
    setReplError(null);
    try {
      await API.repl.runCommand(input);
      onInput("");
    } catch (e: any) {
      setReplError(e.message || "Failed to run command");
    }
  };

  const onReorder = (newOrder: ReplCommand[]) => {
    API.repl.reorderCommands(newOrder.map((c) => c.id));
  };

  const onPlay = () => {
    API.repl.runCommandsById(selectedIds);
  };

  const onExport = () => {
    if (selectedIds.length === 0) return;
    API.repl.formatFlow(selectedIds).then(setFormattedFlow);
  };
  const onCopy = () => {};
  const onDelete = () => {
    showConfirmationDialog();
  };

  const flowText = getFlowText(selectedCommands);

  return (
    <>
      {repl.commands.length > 0 ? (
        <div className="flex flex-col">
          <ReplHeader
            onSelectAll={() => setSelected(repl.commands.map((c) => c.id))}
            onDeselectAll={() => setSelected([])}
            selected={selectedIds.length}
            copyText={flowText}
            onPlay={onPlay}
            onExport={onExport}
            onCopy={onCopy}
            onDelete={onDelete}
          />
          <Reorder.Group
            ref={listRef}
            className="overflow-y-scroll overflow-hidden"
            onReorder={onReorder}
            values={repl.commands}
          >
            {repl.commands.map((command) => (
              <Reorder.Item
                value={command}
                key={command.id}
                transition={{ duration: 0.1 }}
                dragTransition={{ bounceStiffness: 2000, bounceDamping: 100 }}
                onDragStart={() => {
                  setDragging(true);
                }}
                onDragEnd={() => {
                  setTimeout(() => setDragging(false));
                }}
              >
                <CommandRow
                  command={command}
                  selected={selectedIds.includes(command.id)}
                  onClick={() => {
                    if (dragging) return;
                    if (selectedIds.includes(command.id)) {
                      setSelected((prevState) =>
                        prevState.filter((id) => id !== command.id)
                      );
                    } else {
                      setSelected((prevState) => [...prevState, command.id]);
                    }
                  }}
                />
              </Reorder.Item>
            ))}
          </Reorder.Group>
        </div>
      ) : (
        <div className="flex flex-col items-center">
          <img
            src="https://i.giphy.com/media/eJ3sBGzMKRRJKnEPxz/giphy.webp"
            alt="empty"
            className="w-[188px] h-[104px] object-cover rounded-lg mb-4"
          />
          <p className="text-center text-base font-semibold mb-1">
            No commands added yet.
          </p>
          <p className="text-center text-sm text-gray-600 dark:text-gray-300">
            Write command below OR select an element, then a command to add it
          </p>
        </div>
      )}
      <form
        className="absolute bottom-0 left-0 right-0 border-t border-slate-100 shadow-up dark:border-slate-800 px-12 pt-6 pb-8 gap-2 flex flex-col"
        onSubmit={(e: React.FormEvent) => {
          e.preventDefault();
          runCommand();
        }}
      >
        <CommandInput
          setValue={(value) => {
            setReplError(null);
            onInput(value);
          }}
          value={input}
          error={replError}
          placeholder="Enter a command, then press CMD + ENTER to run"
          onSubmit={runCommand}
        />
        <Button
          disabled={!input || !!replError}
          type="submit"
          leftIcon="RiPlayLine"
          size="sm"
          className="w-full"
        >
          Run (CMD + ENTER)
        </Button>
      </form>
      {formattedFlow && (
        <SaveFlowModal
          formattedFlow={formattedFlow}
          onClose={() => {
            setFormattedFlow(null);
          }}
        />
      )}
      <Dialog
        title={`Delete (${selectedIds.length}) command${
          selectedIds.length === 1 ? "" : "s"
        }?`}
        content={`Click confirm to delete the selected command${
          selectedIds.length === 1 ? "" : "s"
        }.`}
      />
    </>
  );
};

export default memo(ReplView);
