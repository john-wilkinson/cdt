/*******************************************************************************
 * Copyright (c) 2010 Verigy and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jens Elmenthaler (Verigy) - Added Full GDB pretty-printing support (bug 302121)
 *******************************************************************************/
package org.eclipse.cdt.dsf.mi.service.command.commands;

import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlDMContext;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;

/**
 * -enable-pretty-printing
 *
 * Enables Python based Pretty printing
 * 
 * @since 4.0
 */
public class MIEnablePrettyPrinting extends MICommand<MIInfo> 
{
	/**
	 * @param dmc
	 */
    public MIEnablePrettyPrinting(ICommandControlDMContext dmc) {
        super(dmc, "-enable-pretty-printing"); //$NON-NLS-1$
    }
}
