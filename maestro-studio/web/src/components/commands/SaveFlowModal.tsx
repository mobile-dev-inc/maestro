import { FormattedFlow } from "../../helpers/models";
import React, { useState } from "react";
import AutosizingTextArea from "./CommandInput";
import { saveAs } from "file-saver";
import { Modal } from "../common/Modal";
import { Button } from "../design-system/button";
import { TextArea } from "../design-system/input";

export const SaveFlowModal = ({
  formattedFlow,
  onClose,
}: {
  formattedFlow: FormattedFlow;
  onClose: () => void;
}) => {
  const [config, setConfig] = useState(formattedFlow.config);
  const [commands, setCommands] = useState(formattedFlow.commands);
  const onSave = () => {
    const content = `${config}\n---\n${commands}`;
    saveAs(
      new Blob([content], { type: "text/yaml;charset=utf-8" }),
      "Flow.yaml"
    );
    onClose();
  };

  return (
    <Modal onClose={onClose}>
      <div className="flex flex-col gap-3 p-8 h-full dark:bg-slate-800 dark:text-white rounded-lg">
        <span className="text-lg font-bold">Save Flow to File</span>
        <div className="flex flex-col h-full rounded gap-2">
          <TextArea
            value={config}
            resize="automatic"
            showResizeIcon={false}
            onChange={(e) => setConfig(e.target.value)}
          />
          <div className="flex-grow">
            <TextArea
              value={commands}
              resize="vertical"
              className="h-full"
              showResizeIcon={false}
              onChange={(e) => setCommands(e.target.value)}
            />
          </div>
        </div>
        <div className="flex justify-end gap-2">
          <Button onClick={onClose} type="button" size="md" variant="tertiary">
            Cancel
          </Button>
          <Button onClick={onSave} type="button" size="md" variant="primary">
            Save
          </Button>
        </div>
      </div>
    </Modal>
  );
};
