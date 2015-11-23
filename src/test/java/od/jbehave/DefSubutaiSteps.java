package od.jbehave;

import net.thucydides.core.annotations.Step;
import net.thucydides.core.annotations.Steps;
import od.pages.ReaderFromFile;
import od.steps.SubutaiSteps;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import java.io.FileNotFoundException;
import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class DefSubutaiSteps {

    @Steps
    SubutaiSteps subutaiSteps;

    @Given("the first user is on the home page of Subutai")
    public void first_user_open_home_page_given() throws FileNotFoundException {
        subutaiSteps.open_mng_h1();
    }

    @Given("the second user is on the home page of Subutai")
    public void second_user_open_home_page_given() throws FileNotFoundException {
        subutaiSteps.open_mng_h2();
    }

    @Given("the user enter login and password: '$login', '$password'")
    public void input_login_and_password_given(String login, String password){
        subutaiSteps.inputLogin(login);
        subutaiSteps.inputPassword(password);
    }

    @Given("the user click on the button: Login")
    public void click_button_login_given(){
        subutaiSteps.clickOnButtonLogin();
    }
}