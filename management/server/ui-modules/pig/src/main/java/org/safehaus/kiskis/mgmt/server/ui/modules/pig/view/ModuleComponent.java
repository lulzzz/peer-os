package org.safehaus.kiskis.mgmt.server.ui.modules.pig.view;

import com.vaadin.ui.*;
import org.safehaus.kiskis.mgmt.server.ui.modules.pig.common.chain.Chain;
import org.safehaus.kiskis.mgmt.server.ui.modules.pig.common.chain.Context;
import org.safehaus.kiskis.mgmt.server.ui.modules.pig.common.command.CommandExecutor;
import org.safehaus.kiskis.mgmt.server.ui.modules.pig.service.ChainManager;
import org.safehaus.kiskis.mgmt.server.ui.modules.pig.service.UILogger;
import org.safehaus.kiskis.mgmt.shared.protocol.*;
import org.safehaus.kiskis.mgmt.shared.protocol.api.ui.CommandListener;

public class ModuleComponent extends CustomComponent implements CommandListener {

    private final String moduleName;

    public ModuleComponent(String moduleName) {

        this.moduleName = moduleName;

        setHeight("100%");

        GridLayout grid = getGrid();
        setCompositionRoot(grid);

        TextArea textArea = getTextArea();
        grid.addComponent(textArea, 1, 0, 9, 9);

        addButtons(grid, textArea);
    }

    private static void addButtons(GridLayout grid, TextArea textArea) {

        ChainManager chainManager = new ChainManager(new UILogger(textArea));

        grid.addComponent(getButton("Check Status", chainManager.STATUS_CHAIN), 0, 0);
        grid.addComponent(getButton("Install", chainManager.INSTALL_CHAIN), 0, 1);
        grid.addComponent(getButton("Remove", chainManager.REMOVE_CHAIN), 0, 2);
    }

    private static Button getButton(String name, final Chain chain) {

        Button button = new Button(name);

        button.addListener(new Button.ClickListener() {
            @Override
            public void buttonClick(Button.ClickEvent event) {
                chain.start(new Context());
            }
        });

        return button;
    }

    private static TextArea getTextArea() {

        TextArea textArea = new TextArea("Log:");
        textArea.setSizeFull();
        textArea.setImmediate(true);
        textArea.setWordwrap(false);

        return textArea;
    }

    private static GridLayout getGrid() {

        GridLayout grid = new GridLayout(10, 10);
        grid.setSizeFull();
        grid.setMargin(true);
        grid.setSpacing(true);

        return grid;
    }

    @Override
    public void onCommand(Response response) {
        CommandExecutor.INSTANCE.onResponse(response);
    }

    @Override
    public String getName() {
        return moduleName;
    }

}