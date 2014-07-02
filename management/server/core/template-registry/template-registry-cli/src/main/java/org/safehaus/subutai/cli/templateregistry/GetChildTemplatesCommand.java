package org.safehaus.subutai.cli.templateregistry;


import java.util.List;

import org.safehaus.subutai.api.templateregistry.Template;
import org.safehaus.subutai.api.templateregistry.TemplateRegistryManager;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;


/**
 * CLI for TemplateRegistryManager.getTemplate command
 */
@Command( scope = "registry", name = "get-child-templates",
        description = "Get child templates by parent template name" )
public class GetChildTemplatesCommand extends OsgiCommandSupport {
    @Argument(index = 0, name = "parent template name", required = true, multiValued = false,
            description = "parent template name")
    String parentTemplateName;

    private TemplateRegistryManager templateRegistryManager;


    public void setTemplateRegistryManager( final TemplateRegistryManager templateRegistryManager ) {
        this.templateRegistryManager = templateRegistryManager;
    }


    @Override
    protected Object doExecute() throws Exception {
        List<Template> templates = templateRegistryManager.getChildTemplates( parentTemplateName );
        if ( templates != null && !templates.isEmpty() ) {
            for ( Template template : templates ) {
                System.out.println( template + "\n" );
            }
        }
        else {
            System.out.println( String.format( "Child templates of %s not found", parentTemplateName ) );
        }

        return null;
    }
}
