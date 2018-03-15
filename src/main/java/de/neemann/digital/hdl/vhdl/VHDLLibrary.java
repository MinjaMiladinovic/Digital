/*
 * Copyright (c) 2017 Helmut Neemann
 * Use of this source code is governed by the GPL v3 license
 * that can be found in the LICENSE file.
 */
package de.neemann.digital.hdl.vhdl;

import de.neemann.digital.core.basic.*;
import de.neemann.digital.core.element.ElementTypeDescription;
import de.neemann.digital.core.extern.External;
import de.neemann.digital.hdl.model.HDLException;
import de.neemann.digital.hdl.model.HDLNode;
import de.neemann.digital.hdl.model.Port;
import de.neemann.digital.hdl.printer.CodePrinter;
import de.neemann.digital.hdl.vhdl.lib.*;
import de.neemann.digital.lang.Lang;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * The library of VHDL entities
 */
public class VHDLLibrary {

    private static final Logger LOGGER = LoggerFactory.getLogger(VHDLLibrary.class);
    private final HashMap<String, VHDLEntity> map;
    private ArrayList<HDLNode> nodeList = new ArrayList<>();

    /**
     * Creates a new instance
     *
     * @throws IOException IOException
     */
    public VHDLLibrary() throws IOException {
        map = new HashMap<>();

        VHDLTemplate operate = new VHDLTemplate("Operate");
        put(And.DESCRIPTION, new VHDLEntityParam(operate, new TempParameter()
                .put("op", "AND")
                .put("inv", false)));
        put(NAnd.DESCRIPTION, new VHDLEntityParam(operate, new TempParameter()
                .put("op", "AND")
                .put("inv", true)));
        put(Or.DESCRIPTION, new VHDLEntityParam(operate, new TempParameter()
                .put("op", "OR")
                .put("inv", false)));
        put(NOr.DESCRIPTION, new VHDLEntityParam(operate, new TempParameter()
                .put("op", "OR")
                .put("inv", true)));
        put(XOr.DESCRIPTION, new VHDLEntityParam(operate, new TempParameter()
                .put("op", "XOR")
                .put("inv", false)));
        put(XNOr.DESCRIPTION, new VHDLEntityParam(operate, new TempParameter()
                .put("op", "XOR")
                .put("inv", true)));

        put(External.DESCRIPTION, new ExternalVHDL());
    }

    private void put(ElementTypeDescription description, VHDLEntity entity) {
        map.put(description.getName(), entity);
    }

    private VHDLEntity getEntity(HDLNode node) throws HDLException {
        String elementName = node.getOrigName();
        VHDLEntity e = map.get(elementName);
        if (e == null) {
            try {
                e = new VHDLTemplate(elementName);
                map.put(elementName, e);
            } catch (IOException ex) {
                ex.printStackTrace();
                LOGGER.info("could not load '" + VHDLTemplate.neededFileName(elementName) + "'");
            }
        }

        if (e == null)
            throw new HDLException(Lang.get("err_vhdlNoEntity_N", elementName));
        return e;
    }

    /**
     * Returns the vhdl name of the given node
     *
     * @param node the node
     * @return the name
     * @throws HDLException HDLException
     */
    public String getName(HDLNode node) throws HDLException {
        if (!nodeList.contains(node)) {
            nodeList.add(node);
            node.setHDLName(getEntity(node).getName(node));
        }
        return node.getHDLName();
    }

    /**
     * Writes the ports to the file
     *
     * @param out  the output stream
     * @param node the node
     * @throws HDLException HDLException
     * @throws IOException  IOException
     */
    public void writeDeclaration(CodePrinter out, HDLNode node) throws HDLException, IOException {
        if (node.isCustom()) {
            out.println("port (").inc();
            Separator semic = new Separator(";\n");
            for (Port p : node.getPorts()) {
                semic.check(out);
                writePort(out, p);
            }
            out.println(" );").dec();
        } else {
            VHDLEntity e = getEntity(node);
            e.writeDeclaration(out, node);
        }
    }

    /**
     * Writes a simple port declaration.
     *
     * @param out the output stream
     * @param p   the port
     * @throws IOException  IOException
     * @throws HDLException HDLException
     */
    public static void writePort(CodePrinter out, Port p) throws IOException, HDLException {
        out.print(p.getName()).print(": ").print(VHDLGenerator.getDirection(p)).print(" ").print(VHDLGenerator.getType(p.getBits()));
    }

    /**
     * Adds all used library components to the vhdl file
     *
     * @param out the pront stream
     * @return number of nodes written
     * @throws HDLException HDLException
     * @throws IOException  IOException
     */
    public int finish(CodePrinter out) throws HDLException, IOException {
        out.println("\n-- library components");
        for (HDLNode n : nodeList) {
            getEntity(n).writeEntity(out, n);
            out.println();
        }
        return nodeList.size();
    }

    /**
     * Writes the generic map to the given stream
     *
     * @param out  the output stream
     * @param node the node
     * @throws HDLException HDLException
     * @throws IOException  IOException
     */
    public void writeGenericMap(CodePrinter out, HDLNode node) throws HDLException, IOException {
        if (!node.isCustom()) {
            VHDLEntity e = getEntity(node);
            e.writeGenericMap(out, node);
        }
    }
}
