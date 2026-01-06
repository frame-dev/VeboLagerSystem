package ch.framedev.lagersystem.actions;

import ch.framedev.lagersystem.guis.CompleteOrderGUI;
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

    public static class CompleteOrderAction implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            CompleteOrderGUI completeOrderGUI = new CompleteOrderGUI();
            completeOrderGUI.display();
        }
    }
}
