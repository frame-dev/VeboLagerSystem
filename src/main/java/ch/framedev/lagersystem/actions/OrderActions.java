package ch.framedev.lagersystem.actions;

import ch.framedev.lagersystem.classes.Order;
import ch.framedev.lagersystem.guis.CompleteOrderGUI;
import ch.framedev.lagersystem.guis.EditOrderGUI;
import ch.framedev.lagersystem.guis.NewOrderGUI;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class OrderActions {

    public static class CreateOrderAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            NewOrderGUI newOrderGUI = new NewOrderGUI();
            newOrderGUI.display();
        }
    }

    public static class DeleteOrderAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            // Logic to delete an order
            System.out.println("Deleting an order...");
        }
    }

    public static class CompleteOrderAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            CompleteOrderGUI completeOrderGUI = new CompleteOrderGUI();
            completeOrderGUI.display();
        }
    }
}
