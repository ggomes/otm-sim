package control.command;

public enum CommandOpenClosed implements InterfaceCommand {
    open,closed;

    @Override
    public String asString() {
        return this.toString();
    }
}
