import { ReactElement, useState } from "react";
import { ReplCommand, ReplCommandStatus } from "../../const/models";
import { Checkbox } from "../design-system/checkbox";
import { TextArea } from "../design-system/input";
import { Icon } from "../design-system/icon";
import { Spinner } from "../design-system/spinner";

export default function CommandRow({
  command,
  selected,
  onClick,
}: {
  command: ReplCommand;
  selected: boolean;
  onClick: () => void;
}) {
  const [value, setValue] = useState(command.yaml);

  return (
    <div
      key={command.id}
      className="relative flex flex-row border-b border-slate-100 dark:border-slate-800 gap-3 py-3 dark:active:bg-slate-900 active:bg-slate-100 cursor-pointer"
      onClick={onClick}
    >
      <Checkbox
        size="sm"
        checked={selected}
        className="pointer-events-none my-0.5"
      />
      <div
        className="flex-grow"
        onClick={(e) => {
          e.stopPropagation();
        }}
      >
        <TextArea
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
        />
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
