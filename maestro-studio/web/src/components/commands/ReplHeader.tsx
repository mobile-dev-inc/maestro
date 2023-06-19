import clsx from "clsx";
import { CopyToClipboard } from "react-copy-to-clipboard";
import { Checkbox } from "../design-system/checkbox";
import { Button } from "../design-system/button";

export default function ReplHeader({
  onSelectAll,
  onDeselectAll,
  selected,
  copyText,
  onPlay,
  onExport,
  onCopy,
  onDelete,
}: {
  onSelectAll: () => void;
  onDeselectAll: () => void;
  selected: number;
  copyText: string;
  onPlay: () => void;
  onExport: () => void;
  onCopy: () => void;
  onDelete: () => void;
}) {
  return (
    <div className="flex justify-between w-full border-b border-slate-100 dark:border-slate-800">
      <div
        className={clsx(
          "py-2",
          selected > 0
            ? "text-gray-900 dark:text-white"
            : "text-gray-400 dark:text-gray-600"
        )}
      >
        <Checkbox
          size="sm"
          checked={selected > 0}
          onChange={(value: boolean) => {
            if (value) {
              onSelectAll();
            } else {
              onDeselectAll();
            }
          }}
          label={selected > 0 ? `${selected} Selected` : "Select All"}
        />
      </div>
      {selected > 0 && (
        <div className="flex gap-0">
          <Button
            variant="quaternary"
            size="sm"
            leftIcon="RiPlayLine"
            onClick={onPlay}
          >
            Play
          </Button>
          <Button
            variant="quaternary"
            size="sm"
            leftIcon="RiUpload2Line"
            onClick={onExport}
          >
            Export
          </Button>
          <CopyToClipboard text={copyText}>
            <Button
              variant="quaternary"
              size="sm"
              leftIcon="RiFileCopyLine"
              onClick={onCopy}
            >
              Copy
            </Button>
          </CopyToClipboard>
          <Button
            variant="quaternary-red"
            size="sm"
            leftIcon="RiDeleteBinLine"
            onClick={onDelete}
          >
            Delete
          </Button>
        </div>
      )}
    </div>
  );
}
