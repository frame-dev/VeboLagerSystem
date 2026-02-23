package ch.framedev.lagersystem.actions;

import ch.framedev.lagersystem.guis.CompleteOrderGUI;
import ch.framedev.lagersystem.guis.NewOrderGUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * The OrderActions class contains inner classes that implement the ActionListener interface to handle actions related to orders in the LagerSystem application. It includes actions for creating a new order and completing an existing order. When the respective action is triggered, it opens the corresponding GUI for the user to interact with.
 * @author framedev
 */
public class OrderActions {

    /**
     * The CreateOrderAction class implements the ActionListener interface and defines the action to be performed when the "Create Order" button is clicked. When the action is triggered, it creates an instance of the NewOrderGUI class and calls its display method to show the GUI for creating a new order.
     */
    public static class CreateOrderAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            NewOrderGUI newOrderGUI = new NewOrderGUI();
            newOrderGUI.display();
        }
    }

    /**
     * The CompleteOrderAction class implements the ActionListener interface and defines the action to be performed when the "Complete Order" button is clicked. When the action is triggered, it creates an instance of the CompleteOrderGUI class and calls its display method to show the GUI for completing an existing order.
     */
    public static class CompleteOrderAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            CompleteOrderGUI completeOrderGUI = new CompleteOrderGUI();
            completeOrderGUI.display();
        }
    }
}
