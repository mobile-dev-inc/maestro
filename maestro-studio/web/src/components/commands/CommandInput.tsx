import { InputHint, InputWrapper, TextArea } from "../design-system/input";
import { TextAreaProps } from "../../helpers/models";

const CommandInput = ({
  value,
  setValue,
  error,
  onSubmit,
  ...rest
}: {
  value: string;
  setValue: (value: string) => void;
  error?: boolean | string | null;
  onSubmit?: () => void;
} & TextAreaProps) => {
  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.metaKey && e.key === "Enter") {
      onSubmit && onSubmit();
      return;
    } else if (e.key === "Tab") {
      e.preventDefault();
      const start = e.currentTarget.selectionStart;
      setValue(
        value.slice(0, start) +
          "    " +
          value.slice(e.currentTarget.selectionEnd)
      );
    }
  };

  return (
    <InputWrapper error={error}>
      <TextArea
        value={value}
        rows={2}
        resize="automatic"
        showResizeIcon={false}
        onChange={(e) => {
          setValue(e.target.value);
        }}
        onKeyDown={handleKeyDown}
        className="font-mono font-normal"
        {...rest}
      />
      <InputHint />
    </InputWrapper>
  );
};

export default CommandInput;
