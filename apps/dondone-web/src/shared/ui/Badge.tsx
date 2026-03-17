type BadgeProps = {
  tone: "success" | "warn" | "danger" | "soft" | "auto";
  children: string;
};

export function Badge({ tone, children }: BadgeProps) {
  return <span className={`badge ${tone}`}>{children}</span>;
}
