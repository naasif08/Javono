package javono.validator;

import javono.utils.*;

public class ValidatorFacade {

    private static final ValidatorFacade INSTANCE = new ValidatorFacade();
    private static final SketchValidator sketchValidator = SketchValidator.getInstance();


    private ValidatorFacade() {
    }


    public static ValidatorFacade getInstance() {
        return INSTANCE;
    }

    public void validateProject() {
        sketchValidator.validateProject();
    }


}
