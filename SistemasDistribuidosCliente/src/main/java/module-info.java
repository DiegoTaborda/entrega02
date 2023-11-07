module sistemasdistribuidos.cliente {
    requires javafx.controls;
    requires javafx.fxml;

    requires json;

    requires org.apache.commons.codec;
    requires jjwt.api;
    requires jjwt.impl;

    opens sistemasdistribuidos.cliente to javafx.fxml;
    exports sistemasdistribuidos.cliente;
}