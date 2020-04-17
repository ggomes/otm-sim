package control.command;

public class CommandNumber implements InterfaceCommand {

    public float value;

    public CommandNumber(float value){
        this.value = value;
    }

    @Override
    public String asString() {
        return String.format("%f", value);
    }

}
