package de.monochromata.jactr.dl;

import org.jactr.core.module.declarative.basic.DefaultAssociativeLinkageSystem;
import org.jactr.core.module.declarative.six.learning.DefaultDeclarativeLearningModule6;

/**
 * A variant of the declarative learning module that uses a
 * {@link CollectionAssociativeLinkageSystem}.
 */
public class CollectionDeclarativeLearningModule6 extends DefaultDeclarativeLearningModule6 {

	@Override
	protected DefaultAssociativeLinkageSystem createAssociativeLinkageSystem() {
		return new CollectionAssociativeLinkageSystem(this, getExecutor());
	}

}
