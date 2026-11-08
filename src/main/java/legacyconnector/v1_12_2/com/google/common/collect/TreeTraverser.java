package legacyconnector.v1_12_2.com.google.common.collect;

import com.google.common.collect.FluentIterable;
import com.google.common.collect.UnmodifiableIterator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;

/**
 * Guava's {@code TreeTraverser}, removed in Guava 33 (replaced by {@code Traverser}); the 1.12.2 era shipped Guava 21 with it. Same traversal
 * orders as the original.
 */
public abstract class TreeTraverser<T> {
    public TreeTraverser() {}

    public static <T> TreeTraverser<T> using(final com.google.common.base.Function<T, ? extends Iterable<T>> nodeToChildrenFunction) {
        if (nodeToChildrenFunction == null) throw new NullPointerException();
        return new TreeTraverser<T>() {
            @Override
            public Iterable<T> children(T root) {
                return nodeToChildrenFunction.apply(root);
            }
        };
    }

    public abstract Iterable<T> children(T root);

    public final FluentIterable<T> preOrderTraversal(final T root) {
        return new FluentIterable<T>() {
            @Override
            public UnmodifiableIterator<T> iterator() {
                return preOrderIterator(root);
            }
        };
    }

    UnmodifiableIterator<T> preOrderIterator(T root) {
        final Deque<Iterator<T>> stack = new ArrayDeque<>();
        List<T> first = new ArrayList<>();
        first.add(root);
        stack.addLast(first.iterator());
        return new UnmodifiableIterator<T>() {
            @Override
            public boolean hasNext() {
                return !stack.isEmpty();
            }

            @Override
            public T next() {
                Iterator<T> top = stack.getLast();
                T result = top.next();
                if (!top.hasNext()) stack.removeLast();
                Iterator<T> kids = children(result).iterator();
                if (kids.hasNext()) stack.addLast(kids);
                return result;
            }
        };
    }

    public final FluentIterable<T> postOrderTraversal(final T root) {
        return new FluentIterable<T>() {
            @Override
            public UnmodifiableIterator<T> iterator() {
                return postOrderIterator(root);
            }
        };
    }

    private static final class PostOrderNode<T> {
        final T root;
        final Iterator<T> childIterator;

        PostOrderNode(T root, Iterator<T> childIterator) {
            this.root = root;
            this.childIterator = childIterator;
        }
    }

    UnmodifiableIterator<T> postOrderIterator(T root) {
        final ArrayDeque<PostOrderNode<T>> stack = new ArrayDeque<>();
        stack.addLast(new PostOrderNode<>(root, children(root).iterator()));
        return new UnmodifiableIterator<T>() {
            @Override
            public boolean hasNext() {
                return !stack.isEmpty();
            }

            @Override
            public T next() {
                while (true) {
                    PostOrderNode<T> top = stack.getLast();
                    if (top.childIterator.hasNext()) {
                        T child = top.childIterator.next();
                        stack.addLast(new PostOrderNode<>(child, children(child).iterator()));
                    } else {
                        stack.removeLast();
                        return top.root;
                    }
                }
            }
        };
    }

    public final FluentIterable<T> breadthFirstTraversal(final T root) {
        return new FluentIterable<T>() {
            @Override
            public UnmodifiableIterator<T> iterator() {
                final Deque<T> queue = new ArrayDeque<>();
                queue.add(root);
                return new UnmodifiableIterator<T>() {
                    @Override
                    public boolean hasNext() {
                        return !queue.isEmpty();
                    }

                    @Override
                    public T next() {
                        T result = queue.remove();
                        for (T child : children(result)) queue.add(child);
                        return result;
                    }
                };
            }
        };
    }
}
