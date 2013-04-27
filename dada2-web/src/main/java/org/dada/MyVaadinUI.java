package org.dada;

import com.vaadin.server.VaadinRequest;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Label;
import com.vaadin.ui.Table;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

/**
 * The Application's "main" class
 */
@SuppressWarnings("serial")
public class MyVaadinUI extends UI
{

    @Override
    protected void init(VaadinRequest request) {
        final VerticalLayout layout = new VerticalLayout();
        layout.setMargin(true);
        setContent(layout);
        
        Button button = new Button("Click Me");
        button.addClickListener(new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
                layout.addComponent(new Label("Thank you for clicking"));
            }
        });
        //layout.addComponent(button);
        /* Create the table with a caption. */
        Table table = new Table("This is my Table");

        /* Define the names and data types of columns.
         * The "default value" parameter is meaningless here. */
        table.addContainerProperty("First Name", String.class,  null);
        table.addContainerProperty("Last Name",  String.class,  null);
        table.addContainerProperty("Year",       Integer.class, null);

        /* Add a few items in the table. */
        table.addItem(new Object[] {
            "Nicolaus","Copernicus",new Integer(1473)}, new Integer(1));
        table.addItem(new Object[] {
            "Tycho",   "Brahe",     new Integer(1546)}, new Integer(2));
        table.addItem(new Object[] {
            "Giordano","Bruno",     new Integer(1548)}, new Integer(3));
        table.addItem(new Object[] {
            "Galileo", "Galilei",   new Integer(1564)}, new Integer(4));
        table.addItem(new Object[] {
            "Johannes","Kepler",    new Integer(1571)}, new Integer(5));
        table.addItem(new Object[] {
            "Isaac",   "Newton",    new Integer(1643)}, new Integer(6));
        layout.addComponent(table);
    }

}
