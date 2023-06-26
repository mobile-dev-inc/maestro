import { ReactElement, useState } from "react";
import { ReplCommand, ReplCommandStatus } from "../../const/models";
import { Checkbox } from "../design-system/checkbox";
import { TextArea } from "../design-system/input";
import { Icon } from "../design-system/icon";
import { Spinner } from "../design-system/spinner";
import clsx from "clsx";
import { twMerge } from "tailwind-merge";
import CommandInput from "./CommandInput";

interface CommandRowProps {
  command: ReplCommand;
  selected: boolean;
  onClick: () => void;
  isDragging: boolean;
}

export default function CommandRow({
  command,
  selected,
  onClick,
  isDragging,
}: CommandRowProps) {
  const [value, setValue] = useState(command.yaml);

  return (
    <div
      className={twMerge(
        clsx(
          "flex flex-row border-b border-slate-100 dark:border-slate-800 gap-3 py-3 flex-grow",
          isDragging && "border-transparent"
        )
      )}
    >
      <div onClick={onClick} className="my-0.5 cursor-pointer">
        <Checkbox
          size="sm"
          checked={selected}
          className="pointer-events-none"
        />
      </div>
      <div
        className="flex-grow"
        onClick={(e) => {
          e.stopPropagation();
        }}
      >
        <CommandInput
          onFocus={(e: React.FocusEvent<HTMLTextAreaElement>) => {
            e.stopPropagation();
          }}
          rows={1}
          className="p-0 bg-transparent border-transparent dark:bg-transparent dark:border-transparent whitespace-nowrap"
          value={value}
          setValue={(val: string) => setValue(val)}
        />
        {/* <TextArea
          onFocus={(e: React.FocusEvent<HTMLTextAreaElement>) => {
            e.stopPropagation();
          }}
          rows={1}
          resize="automatic"
          showResizeIcon={false}
          className="p-0 bg-transparent border-transparent dark:bg-transparent dark:border-transparent whitespace-nowrap"
          value={value}
          onChange={(e: React.ChangeEvent<HTMLTextAreaElement>) =>
            setValue(e.target.value)
          }
        /> */}
      </div>
      <div className="p-1">
        <StatusIcon status={command.status} />
      </div>
    </div>
  );
}

const StatusIcon = ({
  status,
}: {
  status: ReplCommandStatus;
}): ReactElement | null => {
  switch (status) {
    case "success":
      return (
        <Icon iconName="RiCheckLine" size="16" className="text-green-500" />
      );
    case "canceled":
      return null;
    case "error":
      return <Icon iconName="RiAlertLine" size="16" className="text-red-500" />;
    case "pending":
      return null;
    case "running":
      return <Spinner size="16" className="text-gray-900 dark:text-white" />;
  }
};
