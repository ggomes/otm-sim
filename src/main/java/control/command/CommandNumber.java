package control.command;

public class CommandNumber implements InterfaceCommand {

    public Float value;

    public CommandNumber(Float value){
        this.value = value;
    }

    @Override
    public String asString() {
        return String.format("%f", value);
    }

}
