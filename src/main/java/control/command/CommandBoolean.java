package control.command;

public class CommandBoolean implements InterfaceCommand  {
    public boolean value;

    public CommandBoolean(boolean value){
        this.value = value;
    }

    @Override
    public String asString() {
        return value?"true":"false";
    }
}
